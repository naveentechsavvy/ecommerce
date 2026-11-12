package com.apparel.commerce.coupon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;

/**
 * Pure, side-effect-free coupon rules so they can be unit-tested without a database.
 * Returns the reason a coupon is not applicable, or the discount amount.
 */
public final class CouponRules {
    private CouponRules() {}

    public record Context(BigDecimal subtotal, boolean registered, long usesByThisShopper, Instant now) {}

    public sealed interface Result permits Applicable, NotApplicable {}

    public record Applicable(BigDecimal discount) implements Result {}

    public record NotApplicable(String code, String message) implements Result {}

    public static Result evaluate(Coupon c, Context ctx) {
        Optional<NotApplicable> rejection = check(c, ctx);
        if (rejection.isPresent()) return rejection.get();
        return new Applicable(discountFor(c, ctx.subtotal()));
    }

    static Optional<NotApplicable> check(Coupon c, Context ctx) {
        if (!c.isActive()) {
            return reject("COUPON_INACTIVE", "This coupon is no longer active.");
        }
        if (c.getStartsAt() != null && ctx.now().isBefore(c.getStartsAt())) {
            return reject("COUPON_NOT_STARTED", "This coupon is not valid yet.");
        }
        if (c.getEndsAt() != null && !ctx.now().isBefore(c.getEndsAt())) {
            return reject("COUPON_EXPIRED", "This coupon has expired.");
        }
        if (c.getTargetUserType() == Coupon.TargetUserType.REGISTERED && !ctx.registered()) {
            return reject("LOGIN_REQUIRED", "Sign in to use this coupon.");
        }
        if (c.getTargetUserType() == Coupon.TargetUserType.GUEST && ctx.registered()) {
            return reject("GUEST_ONLY", "This coupon is only for guest checkout.");
        }
        if (c.getMinCartValue() != null && ctx.subtotal().compareTo(c.getMinCartValue()) < 0) {
            return reject("MIN_CART_NOT_MET",
                "Add items worth ₹" + c.getMinCartValue().subtract(ctx.subtotal()).setScale(0, RoundingMode.UP)
                    + " more to use this coupon.");
        }
        if (c.getUsageLimitTotal() != null && c.getTimesUsed() >= c.getUsageLimitTotal()) {
            return reject("USAGE_LIMIT_REACHED", "This coupon has been fully redeemed.");
        }
        if (c.getUsageLimitPerUser() != null && ctx.usesByThisShopper() >= c.getUsageLimitPerUser()) {
            return reject("PER_USER_LIMIT_REACHED", "You have already used this coupon the maximum number of times.");
        }
        return Optional.empty();
    }

    public static BigDecimal discountFor(Coupon c, BigDecimal subtotal) {
        BigDecimal discount = switch (c.getDiscountType()) {
            case PERCENTAGE -> subtotal.multiply(c.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FIXED_AMOUNT -> c.getDiscountValue();
        };
        if (c.getMaxDiscountAmount() != null && discount.compareTo(c.getMaxDiscountAmount()) > 0) {
            discount = c.getMaxDiscountAmount();
        }
        return discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);   // never below zero
    }

    private static Optional<NotApplicable> reject(String code, String message) {
        return Optional.of(new NotApplicable(code, message));
    }
}
