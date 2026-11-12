package com.apparel.commerce.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    Page<InventoryMovement> findByVariantIdOrderByCreatedAtDesc(Long variantId, Pageable pageable);
}
