package com.apparel.commerce.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByCategoryId(Long categoryId);

    /**
     * Ranked full-text search over name/brand/material/description
     * (generated tsvector column) with a substring fallback for partial words.
     * Shared by the PLP search box and the AI assistant's search tool.
     */
    @Query(value = """
        SELECT p.id FROM product p
        WHERE p.status = 'ACTIVE'
          AND (p.search_vector @@ websearch_to_tsquery('english', :q)
               OR p.name ILIKE '%' || :q || '%')
        ORDER BY ts_rank(p.search_vector, websearch_to_tsquery('english', :q)) DESC, p.popularity_score DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Long> searchIds(@Param("q") String q, @Param("limit") int limit);

    @Query("select distinct v.size from ProductVariant v where v.active = true and v.product.status = 'ACTIVE' and v.product.category.id in :categoryIds")
    List<String> distinctSizes(@Param("categoryIds") Collection<Long> categoryIds);

    @Query("select distinct v.color from ProductVariant v where v.active = true and v.product.status = 'ACTIVE' and v.product.category.id in :categoryIds order by v.color")
    List<String> distinctColors(@Param("categoryIds") Collection<Long> categoryIds);
}
