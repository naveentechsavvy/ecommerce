package com.apparel.commerce.order;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/** Line snapshot: name/price are copied so later catalog edits never rewrite order history. */
@Entity
@Table(name = "order_item")
@Getter
@Setter
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    private Long variantId;
    private String sku;
    private String productName;
    private String productSlug;
    private String size;
    private String color;
    private String imageUrl;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal lineTotal;
}
