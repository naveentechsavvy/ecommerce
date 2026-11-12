package com.apparel.commerce.coupon;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class CouponRulesTest {

    private static final Instant NOW = Instant.parse("2026-06-01T10:00:00Z");

    private static Coupon pct(int pct, Integer cap) {
        Coupon c = new Coupon();
        c.setCode("TEST");
        c.setDiscountType(Coupon.DiscountType.PERCENTAGE);
        c.setDiscountValue(BigDecimal.valueOf(pct));
        c.setMaxDiscountAmount(cap == null ? null : BigDecimal.valueOf(cap));
        c.setMinCartValue(BigDecimal.ZERO);
        return c;
    }

    private static CouponRules.Context ctx(double subtotal, boolean registered, long uses) {
        return new CouponRules.Context(BigDecimal.valueOf(subtotal), registered, uses, NOW);
    }

    @Test
    void percentageDiscountIsCapped() {
        var r = CouponRules.evaluate(pct(10, 300), ctx(5000, false, 0));
        assertThat(r).isInstanceOf(CouponRules.Applicable.class);
        assertThat(((CouponRules.Applicable) r).discount()).isEqualByComparingTo("300.00");
    }

    @Test
    void percentageDiscountBelowCap() {
        var r = (CouponRules.Applicable) CouponRules.evaluate(pct(10, 300), ctx(1299, false, 0));
        assertThat(r.discount()).isEqualByComparingTo("129.90");
    }

    @Test
    void fixedDiscountNeverExceedsSubtotal() {
        Coupon c = new Coupon();
        c.setDiscountType(Coupon.DiscountType.FIXED_AMOUNT);
        c.setDiscountValue(BigDecimal.valueOf(500));
        c.setMinCartValue(BigDecimal.ZERO);
        var r = (CouponRules.Applicable) CouponRules.evaluate(c, ctx(399, false, 0));
        assertThat(r.discount()).isEqualByComparingTo("399.00");
    }

    @Test
    void minimumCartValueEnforced() {
        Coupon c = pct(10, null);
        c.setMinCartValue(BigDecimal.valueOf(1499));
        var r = CouponRules.evaluate(c, ctx(1000, false, 0));
        assertThat(r).isEqualTo(new CouponRules.NotApplicable("MIN_CART_NOT_MET",
            "Add items worth ₹499 more to use this coupon."));
    }

    @Test
    void targetUserTypeEnforced() {
        Coupon members = pct(20, null);
        members.setTargetUserType(Coupon.TargetUserType.REGISTERED);
        assertThat(code(CouponRules.evaluate(members, ctx(3000, false, 0)))).isEqualTo("LOGIN_REQUIRED");
        assertThat(CouponRules.evaluate(members, ctx(3000, true, 0))).isInstanceOf(CouponRules.Applicable.class);

        Coupon guests = pct(5, null);
        guests.setTargetUserType(Coupon.TargetUserType.GUEST);
        assertThat(code(CouponRules.evaluate(guests, ctx(3000, true, 0)))).isEqualTo("GUEST_ONLY");
    }

    @Test
    void validityWindowAndUsageCaps() {
        Coupon c = pct(10, null);
        c.setEndsAt(NOW.minus(1, ChronoUnit.SECONDS));
        assertThat(code(CouponRules.evaluate(c, ctx(1000, false, 0)))).isEqualTo("COUPON_EXPIRED");

        c.setEndsAt(null);
        c.setStartsAt(NOW.plus(1, ChronoUnit.DAYS));
        assertThat(code(CouponRules.evaluate(c, ctx(1000, false, 0)))).isEqualTo("COUPON_NOT_STARTED");

        c.setStartsAt(null);
        c.setUsageLimitTotal(100);
        c.setTimesUsed(100);
        assertThat(code(CouponRules.evaluate(c, ctx(1000, false, 0)))).isEqualTo("USAGE_LIMIT_REACHED");

        c.setTimesUsed(5);
        c.setUsageLimitPerUser(1);
        assertThat(code(CouponRules.evaluate(c, ctx(1000, false, 1)))).isEqualTo("PER_USER_LIMIT_REACHED");
    }

    @Test
    void inactiveCouponRejected() {
        Coupon c = pct(10, null);
        c.setActive(false);
        assertThat(code(CouponRules.evaluate(c, ctx(1000, false, 0)))).isEqualTo("COUPON_INACTIVE");
    }

    private static String code(CouponRules.Result r) {
        return ((CouponRules.NotApplicable) r).code();
    }
}
