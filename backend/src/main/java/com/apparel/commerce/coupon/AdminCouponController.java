package com.apparel.commerce.coupon;

import com.apparel.commerce.coupon.CouponService.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {

    private final CouponService coupons;

    @GetMapping
    public List<CouponView> list() {
        return coupons.list();
    }

    @GetMapping("/{id}")
    public CouponView get(@PathVariable Long id) {
        return coupons.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CouponView create(@Valid @RequestBody CouponRequest req) {
        return coupons.save(null, req);
    }

    @PutMapping("/{id}")
    public CouponView update(@PathVariable Long id, @Valid @RequestBody CouponRequest req) {
        return coupons.save(id, req);
    }

    @PatchMapping("/{id}/active")
    public CouponView setActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return coupons.setActive(id, Boolean.TRUE.equals(body.get("active")));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        coupons.delete(id);
    }

    @GetMapping("/{id}/redemptions")
    public List<RedemptionView> redemptions(@PathVariable Long id) {
        return coupons.redemptions(id);
    }
}
