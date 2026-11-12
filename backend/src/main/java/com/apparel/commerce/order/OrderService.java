package com.apparel.commerce.order;

import com.apparel.commerce.common.ApiException;
import com.apparel.commerce.common.CurrentUser;
import com.apparel.commerce.common.PageResponse;
import com.apparel.commerce.coupon.CouponService;
import com.apparel.commerce.inventory.InventoryMovement;
import com.apparel.commerce.inventory.InventoryRepository;
import com.apparel.commerce.inventory.InventoryService;
import com.apparel.commerce.order.OrderDtos.*;
import com.apparel.commerce.payment.Payment;
import com.apparel.commerce.payment.PaymentGateway;
import com.apparel.commerce.payment.PaymentRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Order queries for customers/guests/admins and the back-office status workflow. */
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Kolkata");

    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final InventoryService inventory;
    private final InventoryRepository inventoryRepository;
    private final CouponService coupons;
    private final PaymentGateway gateway;

    // ------------------------------------------------------------- customer

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> myOrders(int page, int size) {
        CurrentUser u = CurrentUser.require();
        return PageResponse.of(orders.findByCustomerIdOrderByCreatedAtDesc(u.customerId(),
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50))), OrderMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public OrderView myOrder(String orderNumber) {
        CurrentUser u = CurrentUser.require();
        Order o = find(orderNumber);
        if (!u.customerId().equals(o.getCustomerId())) throw ApiException.notFound("Order", orderNumber);
        return view(o);
    }

    public OrderView cancelMyOrder(String orderNumber) {
        CurrentUser u = CurrentUser.require();
        Order o = find(orderNumber);
        if (!u.customerId().equals(o.getCustomerId())) throw ApiException.notFound("Order", orderNumber);
        if (o.getStatus() != OrderStatus.PAID) {
            throw ApiException.conflict("CANNOT_CANCEL", "This order can no longer be cancelled online. Please contact support.");
        }
        applyStatus(o, OrderStatus.CANCELLED, "Cancelled by customer", null, u.customerId());
        return view(o);
    }

    /** Guest order tracking: order number + checkout email must both match. */
    @Transactional(readOnly = true)
    public OrderView track(String orderNumber, String email) {
        return lookup(orderNumber, email).map(this::view)
            .orElseThrow(() -> ApiException.notFound("Order", orderNumber));
    }

    /** Used by the chatbot tool too; never reveals whether the order number exists if the email differs. */
    @Transactional(readOnly = true)
    public Optional<Order> lookup(String orderNumber, String email) {
        if (orderNumber == null || email == null) return Optional.empty();
        return orders.findByOrderNumber(orderNumber.trim().toUpperCase())
            .filter(o -> o.getEmail().equalsIgnoreCase(email.trim()));
    }

    @Transactional(readOnly = true)
    public Optional<Order> lookupForCustomer(String orderNumber, String customerId) {
        if (orderNumber == null || customerId == null) return Optional.empty();
        return orders.findByOrderNumber(orderNumber.trim().toUpperCase())
            .filter(o -> customerId.equals(o.getCustomerId()));
    }

    // ---------------------------------------------------------------- admin

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> search(OrderStatus status, String q, int page, int size) {
        String term = q == null || q.isBlank() ? null : q.trim();
        return PageResponse.of(orders.search(status, term, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))),
            OrderMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public OrderView adminGet(String orderNumber) {
        return view(find(orderNumber));
    }

    public OrderView changeStatus(String orderNumber, StatusChangeRequest req) {
        Order o = find(orderNumber);
        applyStatus(o, req.status(), req.note(), req.trackingNumber(), CurrentUser.actorName());
        return view(o);
    }

    @Transactional(readOnly = true)
    public DashboardSummary dashboard() {
        Instant startOfDay = LocalDate.now(STORE_ZONE).atStartOfDay(STORE_ZONE).toInstant();
        return new DashboardSummary(
            orders.countByCreatedAtAfter(startOfDay),
            orders.revenueSince(startOfDay, EnumSet.of(OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED)),
            orders.countByStatus(OrderStatus.PAID) + orders.countByStatus(OrderStatus.PROCESSING),
            inventoryRepository.countLowStock(),
            orders.findTop10ByOrderByCreatedAtDesc().stream().map(OrderMapper::toSummary).toList());
    }

    // ---------------------------------------------------------------- shared

    private void applyStatus(Order o, OrderStatus target, String note, String trackingNumber, String actor) {
        OrderStatus current = o.getStatus();
        if (!current.canMoveTo(target)) {
            throw ApiException.conflict("INVALID_TRANSITION", "Cannot move order from " + current + " to " + target);
        }
        if (target == OrderStatus.SHIPPED) {
            if (trackingNumber == null || trackingNumber.isBlank()) {
                throw ApiException.badRequest("TRACKING_REQUIRED", "A tracking number is required to mark as shipped");
            }
            o.setTrackingNumber(trackingNumber.trim());
        }
        var qty = o.quantitiesByVariant();
        if (target == OrderStatus.CANCELLED) {
            if (current.stockCommitted()) {
                inventory.returnToStockAll(qty, o.getOrderNumber(), InventoryMovement.Reason.RETURN);
                refund(o);
            } else if (current == OrderStatus.PENDING_PAYMENT) {
                inventory.releaseAll(qty, o.getOrderNumber());
                if (o.getCouponId() != null) coupons.revoke(o.getCouponId(), o.getId());
            }
        }
        if (target == OrderStatus.RETURNED) {
            inventory.returnToStockAll(qty, o.getOrderNumber(), InventoryMovement.Reason.RETURN);
            refund(o);
        }
        o.transitionTo(target, note, actor);
    }

    private void refund(Order o) {
        payments.findFirstByOrderIdAndStatus(o.getId(), Payment.Status.SUCCEEDED).ifPresent(p -> {
            if (gateway.refund(p.getProviderRef(), p.getAmount())) p.setStatus(Payment.Status.REFUNDED);
        });
    }

    private OrderView view(Order o) {
        return OrderMapper.toView(o, payments.findByOrderIdOrderByCreatedAtDesc(o.getId()));
    }

    private Order find(String orderNumber) {
        return orders.findByOrderNumber(orderNumber).orElseThrow(() -> ApiException.notFound("Order", orderNumber));
    }
}
