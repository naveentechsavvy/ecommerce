package com.apparel.commerce.content;

import com.apparel.commerce.catalog.CatalogDtos.CategoryNode;
import com.apparel.commerce.catalog.CatalogDtos.ProductCard;
import com.apparel.commerce.common.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ContentDtos {
    private ContentDtos() {}

    // storefront
    public record BannerView(Long id, String title, String subtitle, String imageUrl, String mobileImageUrl,
                             String ctaText, String ctaLink) {}

    /** A resolved homepage block: config plus the data it needs, so the homepage renders from one call. */
    public record HomeSectionView(Long id, HomeSection.Type type, String title, Map<String, Object> config,
                                  List<BannerView> banners, List<ProductCard> products, List<CategoryNode> categories) {}

    public record HomePage(List<HomeSectionView> sections) {}

    public record NavNode(Long id, String label, String href, boolean disabled, String badge, List<NavNode> children) {}

    // admin
    public record BannerRequest(@NotBlank @Size(max = 160) String title, String subtitle, @NotBlank String imageUrl,
                                String mobileImageUrl, String ctaText, String ctaLink,
                                @NotNull Banner.Placement placement, Integer sortOrder, Boolean active,
                                Instant startsAt, Instant endsAt) {}

    public record BannerAdminView(Long id, String title, String subtitle, String imageUrl, String mobileImageUrl,
                                  String ctaText, String ctaLink, Banner.Placement placement, int sortOrder,
                                  boolean active, Instant startsAt, Instant endsAt, boolean live) {}

    public record HomeSectionRequest(@NotNull HomeSection.Type sectionType, String title, Map<String, Object> config,
                                     Integer sortOrder, Boolean active) {}

    public record HomeSectionAdminView(Long id, HomeSection.Type sectionType, String title, Map<String, Object> config,
                                       int sortOrder, boolean active, Instant updatedAt) {}

    public record NavItemRequest(Long parentId, @NotBlank @Size(max = 80) String label,
                                 @NotNull NavigationItem.LinkType linkType, Long categoryId, String url,
                                 String badgeText, Integer sortOrder, Visibility visibility) {}

    public record NavItemAdminView(Long id, Long parentId, String label, NavigationItem.LinkType linkType,
                                   Long categoryId, String categoryName, String url, String badgeText, int sortOrder,
                                   Visibility visibility) {}

    public record ReorderRequest(@NotNull List<Long> orderedIds) {}
}
