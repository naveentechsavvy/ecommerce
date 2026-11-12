package com.apparel.commerce.coupon;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "coupon")
@Getter
@Setter
public class Coupon {

    public enum DiscountType { PERCENTAGE, FIXED_AMOUNT }

    public enum TargetUserType { ALL, GUEST, REGISTERED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String description;

    @Enumerated(EnumType.STRING)
    private DiscountType discountType;

    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minCartValue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private TargetUserType targetUserType = TargetUserType.ALL;

    private Instant startsAt;
    private Instant endsAt;
    private Integer usageLimitTotal;
    private Integer usageLimitPerUser;
    private int timesUsed;
    private boolean active = true;

    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
}
