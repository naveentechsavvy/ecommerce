package com.apparel.commerce.coupon;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {

    long countByCouponIdAndCustomerId(Long couponId, String customerId);

    @Query("select count(r) from CouponRedemption r where r.couponId = :couponId and lower(r.email) = lower(:email)")
    long countByCouponIdAndEmail(@Param("couponId") Long couponId, @Param("email") String email);

    List<CouponRedemption> findTop100ByCouponIdOrderByRedeemedAtDesc(Long couponId);

    void deleteByOrderId(Long orderId);
}
