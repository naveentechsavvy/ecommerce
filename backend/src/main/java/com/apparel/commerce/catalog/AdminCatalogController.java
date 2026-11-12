package com.apparel.commerce.catalog;

import com.apparel.commerce.catalog.CatalogDtos.*;
import com.apparel.commerce.common.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Catalog management. Reads are open to ADMIN + OPERATIONS; writes are ADMIN only. */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminCatalogController {

    private final AdminCatalogService admin;

    // categories
    @GetMapping("/categories")
    public List<CategoryAdminView> categories() {
        return admin.listCategories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryAdminView createCategory(@Valid @RequestBody CategoryRequest req) {
        return admin.createCategory(req);
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryAdminView updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        return admin.updateCategory(id, req);
    }

    @PatchMapping("/categories/{id}/visibility")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryAdminView categoryVisibility(@PathVariable Long id, @Valid @RequestBody VisibilityRequest req) {
        return admin.setCategoryVisibility(id, req.visibility());
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCategory(@PathVariable Long id) {
        admin.deleteCategory(id);
    }

    // products
    @GetMapping("/products")
    public PageResponse<ProductAdminRow> products(@RequestParam(required = false) String q,
                                                  @RequestParam(required = false) Long categoryId,
                                                  @RequestParam(required = false) Product.Status status,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return admin.listProducts(q, categoryId, status, page, size);
    }

    @GetMapping("/products/{id}")
    public ProductAdminView product(@PathVariable Long id) {
        return admin.getProduct(id);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductAdminView createProduct(@Valid @RequestBody ProductRequest req) {
        return admin.createProduct(req);
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductAdminView updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return admin.updateProduct(id, req);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void archiveProduct(@PathVariable Long id) {
        admin.archiveProduct(id);
    }

    // variants (SKUs)
    @PostMapping("/products/{id}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductAdminView addVariant(@PathVariable Long id, @Valid @RequestBody VariantRequest req) {
        return admin.addVariant(id, req);
    }

    @PutMapping("/products/{id}/variants/{variantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductAdminView updateVariant(@PathVariable Long id, @PathVariable Long variantId,
                                          @Valid @RequestBody VariantRequest req) {
        return admin.updateVariant(id, variantId, req);
    }

    // images
    @PostMapping("/products/{id}/images")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductAdminView addImage(@PathVariable Long id, @Valid @RequestBody ImageRequest req) {
        return admin.addImage(id, req);
    }

    @DeleteMapping("/products/{id}/images/{imageId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductAdminView removeImage(@PathVariable Long id, @PathVariable Long imageId) {
        return admin.removeImage(id, imageId);
    }

    @GetMapping("/size-charts")
    public List<SizeChartOption> sizeCharts() {
        return admin.sizeCharts();
    }
}
