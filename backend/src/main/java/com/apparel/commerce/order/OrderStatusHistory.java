package com.apparel.commerce.order;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "order_status_history")
@Getter
@Setter
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    private OrderStatus fromStatus;

    @Enumerated(EnumType.STRING)
    private OrderStatus toStatus;

    private String note;
    private String changedBy;
    private Instant changedAt;
}
