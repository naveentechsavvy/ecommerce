package com.apparel.commerce.coupon;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "coupon_redemption")
@Getter
@Setter
public class CouponRedemption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long couponId;
    private Long orderId;
    private String customerId;
    private String email;
    private BigDecimal discountAmount;

    @CreationTimestamp
    private Instant redeemedAt;
}
