package com.apparel.commerce.order;

import com.apparel.commerce.order.OrderDtos.*;
import com.apparel.commerce.payment.Payment;
import java.util.List;

final class OrderMapper {
    private OrderMapper() {}

    static OrderView toView(Order o, List<Payment> payments) {
        return new OrderView(o.getOrderNumber(), o.getStatus(), o.getEmail(), o.getCustomerId() == null,
            o.getItems().stream().map(i -> new OrderItemView(i.getVariantId(), i.getSku(), i.getProductName(),
                i.getProductSlug(), i.getSize(), i.getColor(), i.getImageUrl(), i.getUnitPrice(), i.getQuantity(),
                i.getLineTotal())).toList(),
            o.getSubtotal(), o.getDiscountTotal(), o.getShippingTotal(), o.getTaxTotal(), o.getGrandTotal(),
            o.getCurrency(), o.getCouponCode(), o.getPaymentMethod(),
            payments.stream().map(p -> new PaymentView(p.getMethod(), p.getProvider(), p.getProviderRef(),
                p.getInstrument(), p.getStatus().name(), p.getAmount(), p.getFailureReason(), p.getCreatedAt())).toList(),
            o.getShippingAddress(), o.getBillingAddress(), o.getTrackingNumber(),
            o.getHistory().stream().map(h -> new HistoryView(h.getFromStatus(), h.getToStatus(), h.getNote(),
                h.getChangedBy(), h.getChangedAt())).toList(),
            List.copyOf(o.getStatus().next()), o.getCreatedAt());
    }

    static OrderSummary toSummary(Order o) {
        return new OrderSummary(o.getOrderNumber(), o.getStatus(), o.getEmail(), o.getCustomerId() == null,
            o.getItems().stream().mapToInt(OrderItem::getQuantity).sum(), o.getGrandTotal(), o.getCurrency(),
            o.getCreatedAt());
    }
}
