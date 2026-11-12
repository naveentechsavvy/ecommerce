package com.apparel.commerce.catalog;

import com.apparel.commerce.catalog.CatalogDtos.*;
import com.apparel.commerce.common.ApiException;
import com.apparel.commerce.common.CurrentUser;
import com.apparel.commerce.common.PageResponse;
import com.apparel.commerce.common.SlugUtil;
import com.apparel.commerce.common.Visibility;
import com.apparel.commerce.inventory.Inventory;
import com.apparel.commerce.inventory.InventoryMovement;
import com.apparel.commerce.inventory.InventoryMovementRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Back-office write model for categories, products, variants (SKUs) and images. */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminCatalogService {

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final SizeChartRepository sizeCharts;
    private final InventoryMovementRepository movements;

    // ------------------------------------------------------------ categories

    @Transactional(readOnly = true)
    public List<CategoryAdminView> listCategories() {
        return categories.findAllByOrderBySortOrderAscNameAsc().stream().map(CatalogMapper::toAdmin).toList();
    }

    public CategoryAdminView createCategory(CategoryRequest req) {
        Category c = new Category();
        applyCategory(c, req);
        return CatalogMapper.toAdmin(categories.save(c));
    }

    public CategoryAdminView updateCategory(Long id, CategoryRequest req) {
        Category c = categories.findById(id).orElseThrow(() -> ApiException.notFound("Category", id));
        applyCategory(c, req);
        return CatalogMapper.toAdmin(c);
    }

    public CategoryAdminView setCategoryVisibility(Long id, Visibility visibility) {
        Category c = categories.findById(id).orElseThrow(() -> ApiException.notFound("Category", id));
        c.setVisibility(visibility);
        return CatalogMapper.toAdmin(c);
    }

    public void deleteCategory(Long id) {
        if (categories.existsByParent_Id(id) || products.existsByCategoryId(id)) {
            throw ApiException.conflict("CATEGORY_IN_USE",
                "Category has sub-categories or products. Hide it instead, or move its contents first.");
        }
        categories.deleteById(id);
    }

    private void applyCategory(Category c, CategoryRequest req) {
        c.setName(req.name().trim());
        String slug = blank(req.slug()) ? SlugUtil.slugify(req.name()) : SlugUtil.slugify(req.slug());
        categories.findBySlug(slug).filter(other -> !other.getId().equals(c.getId())).ifPresent(other -> {
            throw ApiException.conflict("SLUG_TAKEN", "Slug already used: " + slug);
        });
        c.setSlug(slug);
        if (req.parentId() != null) {
            if (Objects.equals(req.parentId(), c.getId())) {
                throw ApiException.badRequest("INVALID_PARENT", "A category cannot be its own parent");
            }
            Category parent = categories.findById(req.parentId())
                .orElseThrow(() -> ApiException.notFound("Parent category", req.parentId()));
            for (Category a = parent; a != null; a = a.getParent()) {
                if (c.getId() != null && a.getId().equals(c.getId())) {
                    throw ApiException.badRequest("INVALID_PARENT", "Moving here would create a cycle");
                }
            }
            c.setParent(parent);
        } else {
            c.setParent(null);
        }
        c.setDescription(req.description());
        c.setImageUrl(req.imageUrl());
        c.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        if (req.visibility() != null) c.setVisibility(req.visibility());
    }

    // -------------------------------------------------------------- products

    @Transactional(readOnly = true)
    public PageResponse<ProductAdminRow> listProducts(String q, Long categoryId, Product.Status status, int page, int size) {
        Specification<Product> spec = (root, query, cb) -> cb.conjunction();
        if (!blank(q)) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), like));
        }
        if (categoryId != null) spec = spec.and(ProductSpecifications.inCategories(List.of(categoryId)));
        if (status != null) spec = spec.and(ProductSpecifications.status(status));
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
            Sort.by(Sort.Direction.DESC, "updatedAt"));
        return PageResponse.of(products.findAll(spec, pageable), CatalogMapper::toAdminRow);
    }

    @Transactional(readOnly = true)
    public ProductAdminView getProduct(Long id) {
        return CatalogMapper.toAdmin(find(id));
    }

    public ProductAdminView createProduct(ProductRequest req) {
        Product p = new Product();
        applyProduct(p, req);
        return CatalogMapper.toAdmin(products.save(p));
    }

    public ProductAdminView updateProduct(Long id, ProductRequest req) {
        Product p = find(id);
        applyProduct(p, req);
        return CatalogMapper.toAdmin(p);
    }

    /** Soft delete: products referenced by orders must keep their history. */
    public void archiveProduct(Long id) {
        find(id).setStatus(Product.Status.ARCHIVED);
    }

    private void applyProduct(Product p, ProductRequest req) {
        p.setCategory(categories.findById(req.categoryId())
            .orElseThrow(() -> ApiException.notFound("Category", req.categoryId())));
        p.setSizeChart(req.sizeChartId() == null ? null : sizeCharts.findById(req.sizeChartId())
            .orElseThrow(() -> ApiException.notFound("Size chart", req.sizeChartId())));
        p.setName(req.name().trim());
        String slug = blank(req.slug()) ? SlugUtil.slugify(req.name()) : SlugUtil.slugify(req.slug());
        products.findBySlug(slug).filter(other -> !other.getId().equals(p.getId())).ifPresent(other -> {
            throw ApiException.conflict("SLUG_TAKEN", "Slug already used: " + slug);
        });
        p.setSlug(slug);
        p.setBrand(req.brand());
        p.setDescription(req.description());
        p.setMaterial(req.material());
        p.setCareInstructions(req.careInstructions());
        p.setGender(req.gender());
        p.setBasePrice(req.basePrice());
        if (req.compareAtPrice() != null && req.compareAtPrice().compareTo(req.basePrice()) <= 0) {
            throw ApiException.badRequest("INVALID_PRICE", "Compare-at price must be higher than the selling price");
        }
        p.setCompareAtPrice(req.compareAtPrice());
        if (req.status() != null) p.setStatus(req.status());
        if (req.popularityScore() != null) p.setPopularityScore(req.popularityScore());
        p.setMarketingBadge(blank(req.marketingBadge()) ? null : req.marketingBadge());
        p.setMarketingHtml(req.marketingHtml());
    }

    // -------------------------------------------------------------- variants

    public ProductAdminView addVariant(Long productId, VariantRequest req) {
        Product p = find(productId);
        String sku = req.sku().trim().toUpperCase();
        if (variants.existsBySku(sku)) throw ApiException.conflict("SKU_TAKEN", "SKU already exists: " + sku);
        ProductVariant v = new ProductVariant();
        v.setProduct(p);
        v.setSku(sku);
        applyVariant(v, req);
        Inventory inv = new Inventory();
        inv.setVariant(v);
        int initial = req.initialStock() == null ? 0 : req.initialStock();
        inv.setOnHand(initial);
        v.setInventory(inv);
        p.getVariants().add(v);
        products.flush();
        if (initial > 0) {
            movements.save(new InventoryMovement(v.getId(), initial, InventoryMovement.Reason.RESTOCK, null,
                "Initial stock", CurrentUser.actorName()));
        }
        return CatalogMapper.toAdmin(p);
    }

    public ProductAdminView updateVariant(Long productId, Long variantId, VariantRequest req) {
        Product p = find(productId);
        ProductVariant v = p.getVariants().stream().filter(x -> x.getId().equals(variantId)).findFirst()
            .orElseThrow(() -> ApiException.notFound("Variant", variantId));
        String sku = req.sku().trim().toUpperCase();
        if (!sku.equals(v.getSku()) && variants.existsBySku(sku)) {
            throw ApiException.conflict("SKU_TAKEN", "SKU already exists: " + sku);
        }
        v.setSku(sku);
        applyVariant(v, req);
        return CatalogMapper.toAdmin(p);
    }

    private void applyVariant(ProductVariant v, VariantRequest req) {
        v.setSize(req.size().trim());
        v.setColor(req.color().trim());
        v.setColorHex(req.colorHex());
        v.setPriceOverride(req.priceOverride());
        v.setActive(req.active() == null || req.active());
    }

    // ---------------------------------------------------------------- images

    public ProductAdminView addImage(Long productId, ImageRequest req) {
        Product p = find(productId);
        ProductImage img = new ProductImage();
        img.setProduct(p);
        img.setUrl(req.url());
        img.setAltText(req.altText() == null ? p.getName() : req.altText());
        img.setColor(req.color());
        img.setSortOrder(req.sortOrder() == null ? p.getImages().size() + 1 : req.sortOrder());
        p.getImages().add(img);
        return CatalogMapper.toAdmin(p);
    }

    public ProductAdminView removeImage(Long productId, Long imageId) {
        Product p = find(productId);
        if (!p.getImages().removeIf(i -> i.getId().equals(imageId))) throw ApiException.notFound("Image", imageId);
        return CatalogMapper.toAdmin(p);
    }

    @Transactional(readOnly = true)
    public List<SizeChartOption> sizeCharts() {
        return sizeCharts.findAll().stream().map(s -> new SizeChartOption(s.getId(), s.getName())).toList();
    }

    private Product find(Long id) {
        return products.findById(id).orElseThrow(() -> ApiException.notFound("Product", id));
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
