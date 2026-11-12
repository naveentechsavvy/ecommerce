package com.apparel.commerce.catalog;

import com.apparel.commerce.catalog.CatalogDtos.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class CatalogMapper {
    private CatalogMapper() {}

    static ProductCard toCard(Product p) {
        Map<String, String> colours = new LinkedHashMap<>();
        int available = 0;
        boolean anyLow = false;
        for (ProductVariant v : p.getVariants()) {
            if (!v.isActive()) continue;
            colours.putIfAbsent(v.getColor(), v.getColorHex());
            available += v.available();
            if (v.getInventory() != null && v.getInventory().isLowStock()) anyLow = true;
        }
        return new ProductCard(p.getId(), p.getName(), p.getSlug(), p.getBrand(), p.getBasePrice(),
            p.getCompareAtPrice(), p.primaryImageUrl(), p.getMarketingBadge(), available > 0,
            available > 0 && anyLow, new ArrayList<>(colours.keySet()), new ArrayList<>(colours.values()));
    }

    static String stockStatus(ProductVariant v) {
        if (v.available() <= 0) return "OUT_OF_STOCK";
        if (v.getInventory() != null && v.getInventory().isLowStock()) return "LOW_STOCK";
        return "IN_STOCK";
    }

    static ProductDetail toDetail(Product p) {
        List<VariantView> variants = p.getVariants().stream().filter(ProductVariant::isActive)
            .map(v -> new VariantView(v.getId(), v.getSku(), v.getSize(), v.getColor(), v.getColorHex(),
                v.effectivePrice(), Math.min(v.available(), 10), stockStatus(v)))  // cap exposed stock
            .toList();
        SizeChart sc = p.getSizeChart();
        List<Breadcrumb> crumbs = new ArrayList<>();
        for (Category c = p.getCategory(); c != null; c = c.getParent()) {
            crumbs.add(new Breadcrumb(c.getName(), c.getSlug()));
        }
        Collections.reverse(crumbs);
        return new ProductDetail(p.getId(), p.getName(), p.getSlug(), p.getBrand(), p.getDescription(),
            p.getMaterial(), p.getCareInstructions(), p.getGender(), p.getBasePrice(), p.getCompareAtPrice(),
            p.getMarketingBadge(), p.getMarketingHtml(), p.totalAvailable() > 0,
            p.getImages().stream().map(CatalogMapper::toImage).toList(), variants,
            sc == null ? null : new SizeChartView(sc.getName(), sc.getRows(), sc.getFitNotes()), crumbs);
    }

    static ImageView toImage(ProductImage i) {
        return new ImageView(i.getId(), i.getUrl(), i.getAltText(), i.getColor(), i.getSortOrder());
    }

    static CategoryAdminView toAdmin(Category c) {
        return new CategoryAdminView(c.getId(), c.getParentId(), c.getName(), c.getSlug(), c.getDescription(),
            c.getImageUrl(), c.getSortOrder(), c.getVisibility(), c.getUpdatedAt());
    }

    static ProductAdminView toAdmin(Product p) {
        List<VariantAdminView> variants = p.getVariants().stream().map(v -> new VariantAdminView(
            v.getId(), v.getSku(), v.getSize(), v.getColor(), v.getColorHex(), v.getPriceOverride(), v.isActive(),
            v.getInventory() == null ? 0 : v.getInventory().getOnHand(),
            v.getInventory() == null ? 0 : v.getInventory().getReserved(), v.available())).toList();
        return new ProductAdminView(p.getId(), p.getCategory().getId(), p.getCategory().getName(),
            p.getSizeChart() == null ? null : p.getSizeChart().getId(), p.getName(), p.getSlug(), p.getBrand(),
            p.getDescription(), p.getMaterial(), p.getCareInstructions(), p.getGender(), p.getBasePrice(),
            p.getCompareAtPrice(), p.getStatus(), p.getPopularityScore(), p.getMarketingBadge(),
            p.getMarketingHtml(), p.getImages().stream().map(CatalogMapper::toImage).toList(), variants,
            p.totalAvailable(), p.getCreatedAt(), p.getUpdatedAt());
    }

    static ProductAdminRow toAdminRow(Product p) {
        return new ProductAdminRow(p.getId(), p.getName(), p.getSlug(), p.getCategory().getName(), p.getBasePrice(),
            p.getStatus(), p.getVariants().size(), p.totalAvailable(), p.primaryImageUrl(), p.getUpdatedAt());
    }
}
