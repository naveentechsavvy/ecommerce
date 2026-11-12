package com.apparel.commerce.order;

import java.util.EnumSet;
import java.util.Set;

/** Order lifecycle with the allowed transitions enforced in one place. */
public enum OrderStatus {
    PENDING_PAYMENT, PAYMENT_FAILED, PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED, RETURNED;

    public Set<OrderStatus> next() {
        return switch (this) {
            case PENDING_PAYMENT -> EnumSet.of(PAID, PAYMENT_FAILED, CANCELLED);
            case PAID -> EnumSet.of(PROCESSING, CANCELLED);
            case PROCESSING -> EnumSet.of(SHIPPED, CANCELLED);
            case SHIPPED -> EnumSet.of(DELIVERED);
            case DELIVERED -> EnumSet.of(RETURNED);
            case PAYMENT_FAILED, CANCELLED, RETURNED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canMoveTo(OrderStatus target) {
        return next().contains(target);
    }

    /** Stock has been deducted (sale committed) in these states. */
    public boolean stockCommitted() {
        return this == PAID || this == PROCESSING || this == SHIPPED || this == DELIVERED;
    }
}
