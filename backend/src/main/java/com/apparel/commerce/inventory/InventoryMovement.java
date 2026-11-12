package com.apparel.commerce.inventory;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** Append-only audit trail of every stock change. */
@Entity
@Table(name = "inventory_movement")
@Getter
@Setter
@NoArgsConstructor
public class InventoryMovement {

    public enum Reason { RESTOCK, ADJUSTMENT, DAMAGE, RESERVE, RELEASE, SALE, RETURN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long variantId;
    private int delta;

    @Enumerated(EnumType.STRING)
    private Reason reason;

    private String reference;
    private String note;
    private String createdBy;

    @CreationTimestamp
    private Instant createdAt;

    public InventoryMovement(Long variantId, int delta, Reason reason, String reference, String note, String createdBy) {
        this.variantId = variantId;
        this.delta = delta;
        this.reason = reason;
        this.reference = reference;
        this.note = note;
        this.createdBy = createdBy;
    }
}
