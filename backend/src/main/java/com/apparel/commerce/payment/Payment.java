package com.apparel.commerce.payment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "payment")
@Getter
@Setter
public class Payment {

    public enum Status { INITIATED, SUCCEEDED, FAILED, REFUNDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    @Enumerated(EnumType.STRING)
    private PaymentGateway.Method method;

    private String provider;
    private String providerRef;
    private String instrument;

    @Enumerated(EnumType.STRING)
    private Status status;

    private BigDecimal amount;
    private String failureReason;

    @CreationTimestamp
    private Instant createdAt;
}
