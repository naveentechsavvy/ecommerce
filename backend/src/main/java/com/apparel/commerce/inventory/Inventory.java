package com.apparel.commerce.inventory;

import com.apparel.commerce.catalog.ProductVariant;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Stock ledger per SKU. {@code available = onHand - reserved}.
 * Reservations are taken at checkout and converted to a sale (onHand decremented)
 * once payment succeeds, or released if payment fails / order is cancelled.
 */
@Entity
@Table(name = "inventory")
@Getter
@Setter
public class Inventory {
    @Id
    @Column(name = "variant_id")
    private Long variantId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    private int onHand;
    private int reserved;
    private int lowStockThreshold = 5;

    @Version
    private long version;

    @UpdateTimestamp
    private Instant updatedAt;

    public int available() {
        return Math.max(0, onHand - reserved);
    }

    public boolean isLowStock() {
        return available() > 0 && available() <= lowStockThreshold;
    }
}
