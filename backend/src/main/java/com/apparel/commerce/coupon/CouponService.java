package com.apparel.commerce.coupon;

import com.apparel.commerce.common.ApiException;
import com.apparel.commerce.common.CurrentUser;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository coupons;
    private final CouponRedemptionRepository redemptions;

    /** Outcome of applying a code to a cart. {@code couponId} is set only when applicable. */
    public record Evaluation(String code, boolean applied, BigDecimal discount, String reasonCode, String message,
                             Long couponId) {
        public static Evaluation none() {
            return new Evaluation(null, false, BigDecimal.ZERO, null, null, null);
        }
    }

    @Transactional(readOnly = true)
    public Evaluation evaluate(String rawCode, BigDecimal subtotal, Optional<CurrentUser> user, String email) {
        if (rawCode == null || rawCode.isBlank()) return Evaluation.none();
        String code = rawCode.trim().toUpperCase(Locale.ROOT);
        Optional<Coupon> found = coupons.findByCodeIgnoreCase(code);
        if (found.isEmpty()) {
            return new Evaluation(code, false, BigDecimal.ZERO, "COUPON_NOT_FOUND", "That code isn't valid.", null);
        }
        Coupon c = found.get();
        long uses = usesByShopper(c, user, email);
        var result = CouponRules.evaluate(c, new CouponRules.Context(subtotal, user.isPresent(), uses, Instant.now()));
        if (result instanceof CouponRules.Applicable ok) {
            return new Evaluation(c.getCode(), true, ok.discount(), null, c.getDescription(), c.getId());
        }
        var no = (CouponRules.NotApplicable) result;
        return new Evaluation(c.getCode(), false, BigDecimal.ZERO, no.code(), no.message(), null);
    }

    private long usesByShopper(Coupon c, Optional<CurrentUser> user, String email) {
        if (user.isPresent()) return redemptions.countByCouponIdAndCustomerId(c.getId(), user.get().customerId());
        if (email != null && !email.isBlank()) return redemptions.countByCouponIdAndEmail(c.getId(), email.trim());
        return 0;
    }

    /** Called inside the order transaction once the order row exists. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void redeem(Long couponId, Long orderId, String customerId, String email, BigDecimal discount) {
        if (coupons.consumeUse(couponId) == 0) {
            throw ApiException.conflict("USAGE_LIMIT_REACHED", "This coupon has just been fully redeemed.");
        }
        CouponRedemption r = new CouponRedemption();
        r.setCouponId(couponId);
        r.setOrderId(orderId);
        r.setCustomerId(customerId);
        r.setEmail(email);
        r.setDiscountAmount(discount);
        redemptions.save(r);
    }

    /** Give the use back, e.g. when payment fails or an unpaid order is cancelled. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void revoke(Long couponId, Long orderId) {
        redemptions.deleteByOrderId(orderId);
        coupons.returnUse(couponId);
    }

    // ================================================================= admin

    public record CouponRequest(@NotBlank @Size(max = 40) @Pattern(regexp = "^[A-Za-z0-9_-]+$") String code,
                                @Size(max = 300) String description,
                                @NotNull Coupon.DiscountType discountType,
                                @NotNull @DecimalMin(value = "0.01") BigDecimal discountValue,
                                @DecimalMin("0.0") BigDecimal maxDiscountAmount,
                                @DecimalMin("0.0") BigDecimal minCartValue,
                                Coupon.TargetUserType targetUserType,
                                Instant startsAt, Instant endsAt,
                                @Min(1) Integer usageLimitTotal, @Min(1) Integer usageLimitPerUser,
                                Boolean active) {}

    public record CouponView(Long id, String code, String description, Coupon.DiscountType discountType,
                             BigDecimal discountValue, BigDecimal maxDiscountAmount, BigDecimal minCartValue,
                             Coupon.TargetUserType targetUserType, Instant startsAt, Instant endsAt,
                             Integer usageLimitTotal, Integer usageLimitPerUser, int timesUsed, boolean active,
                             String state, Instant createdAt) {}

    public record RedemptionView(Long id, Long orderId, String customerId, String email, BigDecimal discountAmount,
                                 Instant redeemedAt) {}

    @Transactional(readOnly = true)
    public List<CouponView> list() {
        return coupons.findAllByOrderByCreatedAtDesc().stream().map(CouponService::toView).toList();
    }

    @Transactional(readOnly = true)
    public CouponView get(Long id) {
        return toView(find(id));
    }

    @Transactional
    public CouponView save(Long id, CouponRequest req) {
        if (req.discountType() == Coupon.DiscountType.PERCENTAGE && req.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw ApiException.badRequest("INVALID_DISCOUNT", "Percentage discount cannot exceed 100");
        }
        if (req.startsAt() != null && req.endsAt() != null && !req.endsAt().isAfter(req.startsAt())) {
            throw ApiException.badRequest("INVALID_SCHEDULE", "End time must be after start time");
        }
        String code = req.code().trim().toUpperCase(Locale.ROOT);
        coupons.findByCodeIgnoreCase(code).filter(o -> !o.getId().equals(id)).ifPresent(o -> {
            throw ApiException.conflict("CODE_TAKEN", "Coupon code already exists: " + code);
        });
        Coupon c = id == null ? new Coupon() : find(id);
        c.setCode(code);
        c.setDescription(req.description());
        c.setDiscountType(req.discountType());
        c.setDiscountValue(req.discountValue());
        c.setMaxDiscountAmount(req.discountType() == Coupon.DiscountType.PERCENTAGE ? req.maxDiscountAmount() : null);
        c.setMinCartValue(req.minCartValue() == null ? BigDecimal.ZERO : req.minCartValue());
        c.setTargetUserType(req.targetUserType() == null ? Coupon.TargetUserType.ALL : req.targetUserType());
        c.setStartsAt(req.startsAt());
        c.setEndsAt(req.endsAt());
        c.setUsageLimitTotal(req.usageLimitTotal());
        c.setUsageLimitPerUser(req.usageLimitPerUser());
        c.setActive(req.active() == null || req.active());
        return toView(coupons.save(c));
    }

    @Transactional
    public CouponView setActive(Long id, boolean active) {
        Coupon c = find(id);
        c.setActive(active);
        return toView(c);
    }

    @Transactional
    public void delete(Long id) {
        Coupon c = find(id);
        if (c.getTimesUsed() > 0) {
            throw ApiException.conflict("COUPON_USED", "Coupon has redemptions; deactivate it instead.");
        }
        coupons.delete(c);
    }

    @Transactional(readOnly = true)
    public List<RedemptionView> redemptions(Long couponId) {
        return redemptions.findTop100ByCouponIdOrderByRedeemedAtDesc(couponId).stream()
            .map(r -> new RedemptionView(r.getId(), r.getOrderId(), r.getCustomerId(), r.getEmail(),
                r.getDiscountAmount(), r.getRedeemedAt()))
            .toList();
    }

    private Coupon find(Long id) {
        return coupons.findById(id).orElseThrow(() -> ApiException.notFound("Coupon", id));
    }

    static CouponView toView(Coupon c) {
        Instant now = Instant.now();
        String state;
        if (!c.isActive()) state = "INACTIVE";
        else if (c.getStartsAt() != null && now.isBefore(c.getStartsAt())) state = "SCHEDULED";
        else if (c.getEndsAt() != null && !now.isBefore(c.getEndsAt())) state = "EXPIRED";
        else if (c.getUsageLimitTotal() != null && c.getTimesUsed() >= c.getUsageLimitTotal()) state = "EXHAUSTED";
        else state = "LIVE";
        return new CouponView(c.getId(), c.getCode(), c.getDescription(), c.getDiscountType(), c.getDiscountValue(),
            c.getMaxDiscountAmount(), c.getMinCartValue(), c.getTargetUserType(), c.getStartsAt(), c.getEndsAt(),
            c.getUsageLimitTotal(), c.getUsageLimitPerUser(), c.getTimesUsed(), c.isActive(), state, c.getCreatedAt());
    }
}
