package com.apparel.commerce.inventory;

import com.apparel.commerce.common.ApiException;
import com.apparel.commerce.common.CurrentUser;
import com.apparel.commerce.common.PageResponse;
import com.apparel.commerce.inventory.InventoryMovement.Reason;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventory;
    private final InventoryMovementRepository movements;

    public record InventoryRow(Long variantId, String sku, Long productId, String productName, String size,
                               String color, int onHand, int reserved, int available, int lowStockThreshold,
                               boolean lowStock, Instant updatedAt) {
        static InventoryRow of(Inventory i) {
            var v = i.getVariant();
            return new InventoryRow(i.getVariantId(), v.getSku(), v.getProduct().getId(), v.getProduct().getName(),
                v.getSize(), v.getColor(), i.getOnHand(), i.getReserved(), i.available(),
                i.getLowStockThreshold(), i.available() <= i.getLowStockThreshold(), i.getUpdatedAt());
        }
    }

    public record AdjustmentRequest(@NotNull Integer delta, @NotNull Reason reason, String note, Integer lowStockThreshold) {}

    public record MovementRow(Long id, int delta, Reason reason, String reference, String note, String createdBy, Instant createdAt) {}

    /**
     * Reserve stock for every line or none. Variant ids are processed in sorted
     * order so concurrent checkouts lock rows in the same order (no deadlocks).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void reserveAll(Map<Long, Integer> qtyByVariant, Map<Long, String> skuByVariant, String reference) {
        for (var e : new TreeMap<>(qtyByVariant).entrySet()) {
            if (inventory.reserve(e.getKey(), e.getValue()) == 0) {
                throw ApiException.conflict("OUT_OF_STOCK",
                    "Not enough stock for " + skuByVariant.getOrDefault(e.getKey(), "variant " + e.getKey()));
            }
            movements.save(new InventoryMovement(e.getKey(), -e.getValue(), Reason.RESERVE, reference, null, "checkout"));
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void releaseAll(Map<Long, Integer> qtyByVariant, String reference) {
        for (var e : new TreeMap<>(qtyByVariant).entrySet()) {
            inventory.release(e.getKey(), e.getValue());
            movements.save(new InventoryMovement(e.getKey(), e.getValue(), Reason.RELEASE, reference, null, "checkout"));
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void commitSaleAll(Map<Long, Integer> qtyByVariant, String reference) {
        for (var e : new TreeMap<>(qtyByVariant).entrySet()) {
            if (inventory.commitSale(e.getKey(), e.getValue()) == 0) {
                throw ApiException.conflict("INVENTORY_STATE", "Reservation missing for variant " + e.getKey());
            }
            movements.save(new InventoryMovement(e.getKey(), -e.getValue(), Reason.SALE, reference, null, "checkout"));
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void returnToStockAll(Map<Long, Integer> qtyByVariant, String reference, Reason reason) {
        for (var e : new TreeMap<>(qtyByVariant).entrySet()) {
            inventory.restock(e.getKey(), e.getValue());
            movements.save(new InventoryMovement(e.getKey(), e.getValue(), reason, reference, null, CurrentUser.actorName()));
        }
    }

    // ------------------------------------------------------------- admin API

    @Transactional(readOnly = true)
    public PageResponse<InventoryRow> search(String q, boolean lowStockOnly, Pageable pageable) {
        String term = (q == null || q.isBlank()) ? null : q.trim();
        return PageResponse.of(inventory.search(term, lowStockOnly, pageable), InventoryRow::of);
    }

    @Transactional
    public InventoryRow adjust(Long variantId, AdjustmentRequest req) {
        Inventory inv = inventory.findById(variantId).orElseThrow(() -> ApiException.notFound("Inventory", variantId));
        int newOnHand = inv.getOnHand() + req.delta();
        if (newOnHand < inv.getReserved()) {
            throw ApiException.badRequest("INVALID_ADJUSTMENT",
                "On-hand cannot drop below reserved quantity (" + inv.getReserved() + ")");
        }
        inv.setOnHand(newOnHand);
        if (req.lowStockThreshold() != null) {
            inv.setLowStockThreshold(Math.max(0, req.lowStockThreshold()));
        }
        if (req.delta() != 0) {
            movements.save(new InventoryMovement(variantId, req.delta(), req.reason(), null, req.note(), CurrentUser.actorName()));
        }
        inventory.flush();
        return InventoryRow.of(inv);
    }

    @Transactional(readOnly = true)
    public PageResponse<MovementRow> movements(Long variantId, Pageable pageable) {
        return PageResponse.of(movements.findByVariantIdOrderByCreatedAtDesc(variantId, pageable),
            m -> new MovementRow(m.getId(), m.getDelta(), m.getReason(), m.getReference(), m.getNote(),
                m.getCreatedBy(), m.getCreatedAt()));
    }
}
