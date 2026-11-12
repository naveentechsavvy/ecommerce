package com.apparel.commerce.order;

import com.apparel.commerce.payment.PaymentGateway;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNumber;
    private String customerId;
    private String email;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private String currency;
    private BigDecimal subtotal;
    private BigDecimal discountTotal = BigDecimal.ZERO;
    private BigDecimal shippingTotal = BigDecimal.ZERO;
    private BigDecimal taxTotal = BigDecimal.ZERO;
    private BigDecimal grandTotal;

    private Long couponId;
    private String couponCode;

    @Enumerated(EnumType.STRING)
    private PaymentGateway.Method paymentMethod;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fullName", column = @Column(name = "ship_full_name")),
        @AttributeOverride(name = "phone", column = @Column(name = "ship_phone")),
        @AttributeOverride(name = "line1", column = @Column(name = "ship_line1")),
        @AttributeOverride(name = "line2", column = @Column(name = "ship_line2")),
        @AttributeOverride(name = "city", column = @Column(name = "ship_city")),
        @AttributeOverride(name = "state", column = @Column(name = "ship_state")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "ship_postal_code")),
        @AttributeOverride(name = "country", column = @Column(name = "ship_country"))
    })
    private Address shippingAddress;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fullName", column = @Column(name = "bill_full_name")),
        @AttributeOverride(name = "phone", column = @Column(name = "bill_phone")),
        @AttributeOverride(name = "line1", column = @Column(name = "bill_line1")),
        @AttributeOverride(name = "line2", column = @Column(name = "bill_line2")),
        @AttributeOverride(name = "city", column = @Column(name = "bill_city")),
        @AttributeOverride(name = "state", column = @Column(name = "bill_state")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "bill_postal_code")),
        @AttributeOverride(name = "country", column = @Column(name = "bill_country"))
    })
    private Address billingAddress;

    private String trackingNumber;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt")
    private List<OrderStatusHistory> history = new ArrayList<>();

    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public void transitionTo(OrderStatus next, String note, String actor) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrder(this);
        h.setFromStatus(status);
        h.setToStatus(next);
        h.setNote(note);
        h.setChangedBy(actor);
        h.setChangedAt(Instant.now());
        history.add(h);
        status = next;
    }

    public Map<Long, Integer> quantitiesByVariant() {
        Map<Long, Integer> m = new HashMap<>();
        items.forEach(i -> m.merge(i.getVariantId(), i.getQuantity(), Integer::sum));
        return m;
    }
}
