package com.apparel.commerce.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Stock mutations are single conditional UPDATE statements so that concurrent
 * checkouts can never oversell: the WHERE clause is the guard, and an update
 * count of 0 means "not enough stock".
 */
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Modifying(flushAutomatically = true)
    @Query("""
        update Inventory i set i.reserved = i.reserved + :qty, i.version = i.version + 1
        where i.variantId = :variantId and i.onHand - i.reserved >= :qty
        """)
    int reserve(@Param("variantId") Long variantId, @Param("qty") int qty);

    @Modifying(flushAutomatically = true)
    @Query("""
        update Inventory i set i.reserved = i.reserved - :qty, i.version = i.version + 1
        where i.variantId = :variantId and i.reserved >= :qty
        """)
    int release(@Param("variantId") Long variantId, @Param("qty") int qty);

    @Modifying(flushAutomatically = true)
    @Query("""
        update Inventory i set i.onHand = i.onHand - :qty, i.reserved = i.reserved - :qty, i.version = i.version + 1
        where i.variantId = :variantId and i.reserved >= :qty and i.onHand >= :qty
        """)
    int commitSale(@Param("variantId") Long variantId, @Param("qty") int qty);

    @Modifying(flushAutomatically = true)
    @Query("update Inventory i set i.onHand = i.onHand + :qty, i.version = i.version + 1 where i.variantId = :variantId")
    int restock(@Param("variantId") Long variantId, @Param("qty") int qty);

    @Query(value = """
        select i from Inventory i join fetch i.variant v join fetch v.product p
        where (:q is null or lower(v.sku) like lower(concat('%', cast(:q as String), '%'))
                          or lower(p.name) like lower(concat('%', cast(:q as String), '%')))
          and (:lowStockOnly = false or (i.onHand - i.reserved) <= i.lowStockThreshold)
        """,
        countQuery = """
        select count(i) from Inventory i join i.variant v join v.product p
        where (:q is null or lower(v.sku) like lower(concat('%', cast(:q as String), '%'))
                          or lower(p.name) like lower(concat('%', cast(:q as String), '%')))
          and (:lowStockOnly = false or (i.onHand - i.reserved) <= i.lowStockThreshold)
        """)
    Page<Inventory> search(@Param("q") String q, @Param("lowStockOnly") boolean lowStockOnly, Pageable pageable);

    @Query("select count(i) from Inventory i where (i.onHand - i.reserved) <= i.lowStockThreshold")
    long countLowStock();
}
