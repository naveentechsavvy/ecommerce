package com.apparel.commerce.catalog;

import com.apparel.commerce.catalog.CatalogDtos.*;
import com.apparel.commerce.common.ApiException;
import com.apparel.commerce.common.PageResponse;
import com.apparel.commerce.common.Visibility;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side catalog used by the storefront (and the AI assistant's tools). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private static final int MAX_PAGE_SIZE = 60;

    private final CategoryRepository categories;
    private final ProductRepository products;

    public List<CategoryNode> categoryTree() {
        List<Category> all = categories.findAllByOrderBySortOrderAscNameAsc();
        return buildTree(all, null);
    }

    private List<CategoryNode> buildTree(List<Category> all, Long parentId) {
        return all.stream()
            .filter(c -> Objects.equals(c.getParentId(), parentId) && c.getVisibility() != Visibility.HIDDEN)
            .map(c -> new CategoryNode(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getImageUrl(),
                c.getVisibility(), buildTree(all, c.getId())))
            .toList();
    }

    public CategoryNode category(String slug) {
        Category c = visibleCategory(slug);
        return new CategoryNode(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getImageUrl(),
            c.getVisibility(), buildTree(categories.findAllByOrderBySortOrderAscNameAsc(), c.getId()));
    }

    private Category visibleCategory(String slug) {
        Category c = categories.findBySlug(slug).orElseThrow(() -> ApiException.notFound("Category", slug));
        if (c.getVisibility() != Visibility.ENABLED) {
            throw ApiException.notFound("Category", slug);
        }
        return c;
    }

    /** Category id plus all descendant ids, so "Men" lists products from "Men > Shirts" too. */
    public Set<Long> categoryAndDescendants(Long rootId) {
        List<Category> all = categories.findAll();
        Map<Long, List<Long>> children = new HashMap<>();
        for (Category c : all) {
            if (c.getParentId() != null && c.getVisibility() == Visibility.ENABLED) {
                children.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c.getId());
            }
        }
        Set<Long> result = new LinkedHashSet<>();
        Deque<Long> stack = new ArrayDeque<>(List.of(rootId));
        while (!stack.isEmpty()) {
            Long id = stack.pop();
            if (result.add(id)) stack.addAll(children.getOrDefault(id, List.of()));
        }
        return result;
    }

    public PageResponse<ProductCard> listProducts(ProductFilter f, int page, int size) {
        Specification<Product> spec = ProductSpecifications.status(Product.Status.ACTIVE);
        if (f.category() != null && !f.category().isBlank()) {
            spec = spec.and(ProductSpecifications.inCategories(categoryAndDescendants(visibleCategory(f.category()).getId())));
        }
        if (f.q() != null && !f.q().isBlank()) {
            List<Long> ids = products.searchIds(f.q().trim(), 500);
            if (ids.isEmpty()) {
                return new PageResponse<>(List.of(), page, size, 0, 0);
            }
            spec = spec.and(ProductSpecifications.idIn(ids));
        }
        spec = spec.and(ProductSpecifications.priceBetween(f.minPrice(), f.maxPrice()));
        boolean inStockOnly = Boolean.TRUE.equals(f.inStock());
        if (inStockOnly || notEmpty(f.sizes()) || notEmpty(f.colors())) {
            spec = spec.and(ProductSpecifications.hasVariant(f.sizes(), f.colors(), inStockOnly));
        }
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sortOf(f.sort()));
        Page<Product> result = products.findAll(spec, pageable);
        return PageResponse.of(result, CatalogMapper::toCard);
    }

    static Sort sortOf(String sort) {
        if (sort == null) return Sort.by(Sort.Direction.DESC, "createdAt");
        return switch (sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "basePrice").and(Sort.by("id"));
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "basePrice").and(Sort.by("id"));
            case "popularity" -> Sort.by(Sort.Direction.DESC, "popularityScore").and(Sort.by("id"));
            default -> Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"));
        };
    }

    public ProductDetail product(String slug) {
        Product p = products.findBySlug(slug)
            .filter(x -> x.getStatus() == Product.Status.ACTIVE)
            .orElseThrow(() -> ApiException.notFound("Product", slug));
        return CatalogMapper.toDetail(p);
    }

    public Facets facets(String categorySlug) {
        Collection<Long> ids = categorySlug == null || categorySlug.isBlank()
            ? categories.findAll().stream().map(Category::getId).toList()
            : categoryAndDescendants(visibleCategory(categorySlug).getId());
        if (ids.isEmpty()) return new Facets(List.of(), List.of());
        List<String> sizes = new ArrayList<>(products.distinctSizes(ids));
        sizes.sort(Comparator.comparingInt(CatalogService::sizeRank).thenComparing(Comparator.naturalOrder()));
        return new Facets(sizes, products.distinctColors(ids));
    }

    private static final List<String> SIZE_ORDER = List.of("XXS", "XS", "S", "M", "L", "XL", "XXL", "3XL");

    static int sizeRank(String s) {
        int i = SIZE_ORDER.indexOf(s);
        if (i >= 0) return i;
        String digits = s.replaceAll("\\D", "");
        return digits.isEmpty() ? 1000 : 100 + Integer.parseInt(digits);
    }

    private static boolean notEmpty(Collection<?> c) {
        return c != null && !c.isEmpty();
    }
}
