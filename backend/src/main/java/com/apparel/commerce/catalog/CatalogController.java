package com.apparel.commerce.catalog;

import com.apparel.commerce.catalog.CatalogDtos.*;
import com.apparel.commerce.common.PageResponse;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalog;

    @GetMapping("/categories")
    public List<CategoryNode> categories() {
        return catalog.categoryTree();
    }

    @GetMapping("/categories/{slug}")
    public CategoryNode category(@PathVariable String slug) {
        return catalog.category(slug);
    }

    /**
     * PLP: /products?category=men&size=M&size=L&color=Black&minPrice=500&maxPrice=2000&inStock=true&sort=price_asc
     */
    @GetMapping("/products")
    public PageResponse<ProductCard> products(@RequestParam(required = false) String category,
                                              @RequestParam(required = false) String q,
                                              @RequestParam(required = false) BigDecimal minPrice,
                                              @RequestParam(required = false) BigDecimal maxPrice,
                                              @RequestParam(name = "size", required = false) List<String> sizes,
                                              @RequestParam(name = "color", required = false) List<String> colors,
                                              @RequestParam(required = false) Boolean inStock,
                                              @RequestParam(defaultValue = "newest") String sort,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "24") int pageSize) {
        return catalog.listProducts(new ProductFilter(category, q, minPrice, maxPrice, sizes, colors, inStock, sort),
            page, pageSize);
    }

    @GetMapping("/products/{slug}")
    public ProductDetail product(@PathVariable String slug) {
        return catalog.product(slug);
    }

    @GetMapping("/facets")
    public Facets facets(@RequestParam(required = false) String category) {
        return catalog.facets(category);
    }
}
