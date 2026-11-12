package com.apparel.commerce.catalog;

import com.apparel.commerce.common.Visibility;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Request/response contracts for catalog endpoints (storefront + admin). */
public final class CatalogDtos {
    private CatalogDtos() {}

    // ------------------------------------------------------------- storefront

    public record CategoryNode(Long id, String name, String slug, String description, String imageUrl,
                               Visibility visibility, List<CategoryNode> children) {}

    public record ProductCard(Long id, String name, String slug, String brand, BigDecimal price,
                              BigDecimal compareAtPrice, String imageUrl, String badge, boolean inStock,
                              boolean lowStock, List<String> colors, List<String> colorHexes) {}

    public record VariantView(Long id, String sku, String size, String color, String colorHex, BigDecimal price,
                              int available, String stockStatus) {}

    public record ImageView(Long id, String url, String altText, String color, int sortOrder) {}

    public record SizeChartView(String name, List<Map<String, Object>> rows, String fitNotes) {}

    public record Breadcrumb(String name, String slug) {}

    public record ProductDetail(Long id, String name, String slug, String brand, String description, String material,
                                String careInstructions, Product.Gender gender, BigDecimal price,
                                BigDecimal compareAtPrice, String badge, String marketingHtml, boolean inStock,
                                List<ImageView> images, List<VariantView> variants, SizeChartView sizeChart,
                                List<Breadcrumb> breadcrumbs) {}

    public record Facets(List<String> sizes, List<String> colors) {}

    /** PLP query. sort: newest | price_asc | price_desc | popularity */
    public record ProductFilter(String category, String q, BigDecimal minPrice, BigDecimal maxPrice,
                                List<String> sizes, List<String> colors, Boolean inStock, String sort) {}

    // ------------------------------------------------------------------ admin

    public record CategoryRequest(@NotBlank @Size(max = 120) String name, @Size(max = 140) String slug,
                                  Long parentId, String description, String imageUrl, Integer sortOrder,
                                  Visibility visibility) {}

    public record CategoryAdminView(Long id, Long parentId, String name, String slug, String description,
                                    String imageUrl, int sortOrder, Visibility visibility, Instant updatedAt) {}

    public record VisibilityRequest(@NotNull Visibility visibility) {}

    public record ProductRequest(@NotNull Long categoryId, Long sizeChartId, @NotBlank @Size(max = 200) String name,
                                 @Size(max = 220) String slug, String brand, String description, String material,
                                 String careInstructions, Product.Gender gender,
                                 @NotNull @DecimalMin("0.0") BigDecimal basePrice,
                                 @DecimalMin("0.0") BigDecimal compareAtPrice, Product.Status status,
                                 Integer popularityScore, String marketingBadge, String marketingHtml) {}

    public record VariantRequest(@NotBlank @Size(max = 64) String sku, @NotBlank String size, @NotBlank String color,
                                 String colorHex, @DecimalMin("0.0") BigDecimal priceOverride, Boolean active,
                                 @Min(0) Integer initialStock) {}

    public record ImageRequest(@NotBlank String url, String altText, String color, Integer sortOrder) {}

    public record VariantAdminView(Long id, String sku, String size, String color, String colorHex,
                                   BigDecimal priceOverride, boolean active, int onHand, int reserved, int available) {}

    public record ProductAdminView(Long id, Long categoryId, String categoryName, Long sizeChartId, String name,
                                   String slug, String brand, String description, String material,
                                   String careInstructions, Product.Gender gender, BigDecimal basePrice,
                                   BigDecimal compareAtPrice, Product.Status status, int popularityScore,
                                   String marketingBadge, String marketingHtml, List<ImageView> images,
                                   List<VariantAdminView> variants, int totalAvailable, Instant createdAt,
                                   Instant updatedAt) {}

    public record ProductAdminRow(Long id, String name, String slug, String categoryName, BigDecimal basePrice,
                                  Product.Status status, int variantCount, int totalAvailable, String imageUrl,
                                  Instant updatedAt) {}

    public record SizeChartOption(Long id, String name) {}
}
