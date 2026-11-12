package com.apparel.commerce.catalog;

import com.apparel.commerce.inventory.Inventory;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.util.Collection;
import org.springframework.data.jpa.domain.Specification;

/** Composable JPA criteria for the product listing page. */
final class ProductSpecifications {
    private ProductSpecifications() {}

    static Specification<Product> status(Product.Status status) {
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    static Specification<Product> inCategories(Collection<Long> categoryIds) {
        return (root, q, cb) -> root.get("category").get("id").in(categoryIds);
    }

    static Specification<Product> idIn(Collection<Long> ids) {
        return (root, q, cb) -> root.get("id").in(ids);
    }

    static Specification<Product> priceBetween(BigDecimal min, BigDecimal max) {
        return (root, q, cb) -> {
            if (min != null && max != null) return cb.between(root.get("basePrice"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("basePrice"), min);
            if (max != null) return cb.lessThanOrEqualTo(root.get("basePrice"), max);
            return cb.conjunction();
        };
    }

    /** Product has at least one active variant matching size/colour (and in stock when requested). */
    static Specification<Product> hasVariant(Collection<String> sizes, Collection<String> colors, boolean inStockOnly) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<ProductVariant> v = sub.from(ProductVariant.class);
            var predicate = cb.and(cb.equal(v.get("product"), root), cb.isTrue(v.get("active")));
            if (sizes != null && !sizes.isEmpty()) predicate = cb.and(predicate, v.get("size").in(sizes));
            if (colors != null && !colors.isEmpty()) predicate = cb.and(predicate, v.get("color").in(colors));
            if (inStockOnly) {
                Join<ProductVariant, Inventory> inv = v.join("inventory");
                predicate = cb.and(predicate, cb.greaterThan(cb.diff(inv.<Integer>get("onHand"), inv.<Integer>get("reserved")), 0));
            }
            sub.select(v.get("id")).where(predicate);
            return cb.exists(sub);
        };
    }
}
