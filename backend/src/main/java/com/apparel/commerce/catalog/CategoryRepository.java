package com.apparel.commerce.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);

    List<Category> findAllByOrderBySortOrderAscNameAsc();

    boolean existsByParent_Id(Long parentId);
}
