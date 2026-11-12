package com.apparel.commerce.catalog;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    boolean existsBySku(String sku);

    @Query("select v from ProductVariant v join fetch v.product p left join fetch v.inventory where v.id in :ids")
    List<ProductVariant> findAllWithProductByIdIn(@Param("ids") Collection<Long> ids);
}
