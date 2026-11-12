package com.apparel.commerce.coupon;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    List<Coupon> findAllByOrderByCreatedAtDesc();

    /** Atomically consume one global use; returns 0 when the cap is already reached. */
    @Modifying(flushAutomatically = true)
    @Query("""
        update Coupon c set c.timesUsed = c.timesUsed + 1
        where c.id = :id and (c.usageLimitTotal is null or c.timesUsed < c.usageLimitTotal)
        """)
    int consumeUse(@Param("id") Long id);

    @Modifying(flushAutomatically = true)
    @Query("update Coupon c set c.timesUsed = c.timesUsed - 1 where c.id = :id and c.timesUsed > 0")
    int returnUse(@Param("id") Long id);
}
