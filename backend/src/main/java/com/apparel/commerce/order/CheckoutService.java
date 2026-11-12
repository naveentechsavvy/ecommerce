package com.apparel.commerce.order;

import com.apparel.commerce.common.ApiException;
import com.apparel.commerce.common.CurrentUser;
import com.apparel.commerce.coupon.CouponService;
import com.apparel.commerce.inventory.InventoryService;
import com.apparel.commerce.order.OrderDtos.*;
import com.apparel.commerce.payment.Payment;
import com.apparel.commerce.payment.PaymentGateway;
import com.apparel.commerce.payment.PaymentRepository;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checkout orchestration:
 * <pre>
 *  quote (re-price) -> reserve stock -> create order (PENDING_PAYMENT) -> redeem coupon
 *    -> charge PSP -> success: commit sale, PAID
 *                  -> failure: release stock, revoke coupon, PAYMENT_FAILED
 * </pre>
 * The mock PSP answers synchronously. With a real PSP the charge step becomes
 * "create payment intent" and the success/failure branches run from the
 * provider's webhook (see docs/ARCHITECTURE.md, Phase 2).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyMMdd");
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PricingService pricing;
    private final InventoryService inventory;
    private final CouponService coupons;
    private final PaymentGateway gateway;
    private final OrderRepository orders;
    private final PaymentRepository payments;

    /** Short-lived idempotency guard (single node). Use a DB unique key or Redis in a cluster. */
    private final Map<String, String> idempotency = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public Quote quote(QuoteRequest req) {
        return pricing.quote(req.items(), req.couponCode(), CurrentUser.get(), req.email());
    }

    @Transactional(readOnly = true)
    public CouponService.Evaluation validateCoupon(CouponValidateRequest req) {
        return pricing.quote(req.items(), req.code(), CurrentUser.get(), req.email()).coupon();
    }

    @Transactional
    public OrderView placeOrder(PlaceOrderRequest req) {
        Optional<CurrentUser> user = CurrentUser.get();
        String idemKey = req.idempotencyKey() == null ? null
            : user.map(CurrentUser::customerId).orElse("guest") + ":" + req.idempotencyKey();
        if (idemKey != null && idempotency.containsKey(idemKey)) {
            return orders.findByOrderNumber(idempotency.get(idemKey))
                .map(o -> OrderMapper.toView(o, payments.findByOrderIdOrderByCreatedAtDesc(o.getId())))
                .orElseThrow();
        }

        String email = user.map(CurrentUser::email).filter(e -> !e.isBlank()).orElse(req.email());
        if (email == null || email.isBlank()) {
            throw ApiException.badRequest("EMAIL_REQUIRED", "An email address is required for order updates.");
        }

        // 1. Re-price on the server
        Quote quote = pricing.quote(req.items(), req.couponCode(), user, email);
        if (!quote.valid()) {
            throw new ApiException(HttpStatus.CONFLICT, "CART_INVALID", String.join(" ", quote.issues()));
        }
        if (req.couponCode() != null && !req.couponCode().isBlank() && !quote.coupon().applied()) {
            throw ApiException.badRequest(quote.coupon().reasonCode(), quote.coupon().message());
        }

        // 2. Build order snapshot
        Order order = new Order();
        order.setOrderNumber(newOrderNumber());
        order.setCustomerId(user.map(CurrentUser::customerId).orElse(null));
        order.setEmail(email.trim());
        order.setCurrency(quote.currency());
        order.setSubtotal(quote.subtotal());
        order.setDiscountTotal(quote.discount());
        order.setShippingTotal(quote.shipping());
        order.setTaxTotal(quote.tax());
        order.setGrandTotal(quote.total());
        order.setPaymentMethod(req.payment().method());
        if (quote.coupon().applied()) {
            order.setCouponId(quote.coupon().couponId());
            order.setCouponCode(quote.coupon().code());
        }
        order.setShippingAddress(req.shippingAddress());
        Address billing = req.billingSameAsShipping() || req.billingAddress() == null
            ? copy(req.shippingAddress()) : req.billingAddress();
        order.setBillingAddress(billing);
        for (QuoteLine l : quote.lines()) {
            OrderItem item = new OrderItem();
            item.setVariantId(l.variantId());
            item.setSku(l.sku());
            item.setProductName(l.productName());
            item.setProductSlug(l.productSlug());
            item.setSize(l.size());
            item.setColor(l.color());
            item.setImageUrl(l.imageUrl());
            item.setUnitPrice(l.unitPrice());
            item.setQuantity(l.quantity());
            item.setLineTotal(l.lineTotal());
            order.addItem(item);
        }
        order.transitionTo(OrderStatus.PENDING_PAYMENT, "Order placed", user.map(CurrentUser::customerId).orElse("guest"));

        // 3. Reserve stock atomically (throws 409 OUT_OF_STOCK and rolls everything back)
        Map<Long, Integer> qty = order.quantitiesByVariant();
        Map<Long, String> skus = quote.lines().stream().collect(Collectors.toMap(QuoteLine::variantId, QuoteLine::sku));
        inventory.reserveAll(qty, skus, order.getOrderNumber());

        orders.save(order);

        // 4. Consume coupon
        if (order.getCouponId() != null) {
            coupons.redeem(order.getCouponId(), order.getId(), order.getCustomerId(), order.getEmail(), order.getDiscountTotal());
        }

        // 5. Charge
        PaymentGateway.ChargeResult result = gateway.charge(new PaymentGateway.ChargeCommand(
            order.getOrderNumber(), order.getGrandTotal(), order.getCurrency(), order.getEmail(), req.payment()));
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setMethod(req.payment().method());
        payment.setProvider(result.provider());
        payment.setProviderRef(result.providerRef());
        payment.setInstrument(result.maskedInstrument());
        payment.setAmount(order.getGrandTotal());

        if (result.success()) {
            inventory.commitSaleAll(qty, order.getOrderNumber());
            payment.setStatus(Payment.Status.SUCCEEDED);
            order.transitionTo(OrderStatus.PAID, "Payment captured (" + result.provider() + ")", "payment-gateway");
        } else {
            inventory.releaseAll(qty, order.getOrderNumber());
            if (order.getCouponId() != null) coupons.revoke(order.getCouponId(), order.getId());
            payment.setStatus(Payment.Status.FAILED);
            payment.setFailureReason(result.failureReason());
            order.transitionTo(OrderStatus.PAYMENT_FAILED, result.failureReason(), "payment-gateway");
        }
        payments.save(payment);
        if (idemKey != null) idempotency.put(idemKey, order.getOrderNumber());
        log.info("Order {} -> {} ({} {})", order.getOrderNumber(), order.getStatus(), order.getGrandTotal(), order.getCurrency());
        return OrderMapper.toView(order, List.of(payment));
    }

    private static Address copy(Address a) {
        return new Address(a.getFullName(), a.getPhone(), a.getLine1(), a.getLine2(), a.getCity(), a.getState(),
            a.getPostalCode(), a.getCountry());
    }

    static String newOrderNumber() {
        StringBuilder sb = new StringBuilder("AC").append(LocalDate.now(ZoneOffset.UTC).format(DAY)).append('-');
        for (int i = 0; i < 6; i++) sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        return sb.toString();
    }
}
