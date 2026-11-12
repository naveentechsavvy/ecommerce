package com.apparel.commerce.catalog;

import com.apparel.commerce.inventory.Inventory;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "product_variant")
@Getter
@Setter
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    private String sku;
    private String size;
    private String color;
    private String colorHex;
    private BigDecimal priceOverride;
    private boolean active = true;

    @OneToOne(mappedBy = "variant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Inventory inventory;

    public BigDecimal effectivePrice() {
        return priceOverride != null ? priceOverride : product.getBasePrice();
    }

    public int available() {
        return inventory == null ? 0 : inventory.available();
    }
}
