package com.apparel.commerce.order;

import com.apparel.commerce.coupon.CouponService;
import com.apparel.commerce.payment.PaymentGateway;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {
    private OrderDtos() {}

    public record CartLine(@NotNull Long variantId, @NotNull @Min(1) @Max(10) Integer quantity) {}

    public record QuoteRequest(@NotEmpty @Size(max = 50) List<@Valid CartLine> items, String couponCode,
                               @Email String email) {}

    public record CouponValidateRequest(@NotBlank String code, @NotEmpty List<@Valid CartLine> items,
                                        @Email String email) {}

    public record QuoteLine(Long variantId, String sku, String productName, String productSlug, String size,
                            String color, String imageUrl, BigDecimal unitPrice, int quantity, BigDecimal lineTotal,
                            int available, String issue) {}

    public record Quote(List<QuoteLine> lines, BigDecimal subtotal, BigDecimal discount, BigDecimal shipping,
                        BigDecimal tax, BigDecimal total, String currency, CouponService.Evaluation coupon,
                        List<String> issues) {
        public boolean valid() {
            return issues.isEmpty();
        }
    }

    public record PlaceOrderRequest(@NotEmpty @Size(max = 50) List<@Valid CartLine> items,
                                    String couponCode,
                                    @Email String email,
                                    @NotNull @Valid Address shippingAddress,
                                    @Valid Address billingAddress,
                                    boolean billingSameAsShipping,
                                    @NotNull PaymentGateway.PaymentRequest payment,
                                    /** Client-generated UUID; resubmits with the same key return the same order. */
                                    @Size(max = 64) String idempotencyKey) {}

    public record OrderItemView(Long variantId, String sku, String productName, String productSlug, String size,
                                String color, String imageUrl, BigDecimal unitPrice, int quantity,
                                BigDecimal lineTotal) {}

    public record HistoryView(OrderStatus fromStatus, OrderStatus toStatus, String note, String changedBy,
                              Instant changedAt) {}

    public record PaymentView(PaymentGateway.Method method, String provider, String providerRef, String instrument,
                              String status, BigDecimal amount, String failureReason, Instant createdAt) {}

    public record OrderView(String orderNumber, OrderStatus status, String email, boolean guest,
                            List<OrderItemView> items, BigDecimal subtotal, BigDecimal discountTotal,
                            BigDecimal shippingTotal, BigDecimal taxTotal, BigDecimal grandTotal, String currency,
                            String couponCode, PaymentGateway.Method paymentMethod, List<PaymentView> payments,
                            Address shippingAddress, Address billingAddress, String trackingNumber,
                            List<HistoryView> history, List<OrderStatus> allowedNextStatuses, Instant createdAt) {}

    public record OrderSummary(String orderNumber, OrderStatus status, String email, boolean guest, int itemCount,
                               BigDecimal grandTotal, String currency, Instant createdAt) {}

    public record StatusChangeRequest(@NotNull OrderStatus status, @Size(max = 300) String note,
                                      @Size(max = 80) String trackingNumber) {}

    public record DashboardSummary(long ordersToday, BigDecimal revenueToday, long awaitingFulfilment,
                                   long lowStockSkus, List<OrderSummary> recentOrders) {}
}
