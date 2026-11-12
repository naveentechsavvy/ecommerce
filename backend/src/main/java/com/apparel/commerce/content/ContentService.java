package com.apparel.commerce.content;

import com.apparel.commerce.catalog.CatalogDtos.CategoryNode;
import com.apparel.commerce.catalog.CatalogDtos.ProductCard;
import com.apparel.commerce.catalog.CatalogDtos.ProductFilter;
import com.apparel.commerce.catalog.CatalogService;
import com.apparel.commerce.catalog.Category;
import com.apparel.commerce.catalog.CategoryRepository;
import com.apparel.commerce.common.ApiException;
import com.apparel.commerce.common.Visibility;
import com.apparel.commerce.content.ContentDtos.*;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CMS: storefront reads (resolved) and admin writes for banners, homepage layout and the mega menu. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ContentService {

    private final BannerRepository banners;
    private final HomeSectionRepository sections;
    private final NavigationItemRepository navigation;
    private final CategoryRepository categories;
    private final CatalogService catalog;

    // ============================================================ storefront

    @Transactional(readOnly = true)
    public HomePage homePage() {
        List<CategoryNode> tree = null;
        List<HomeSectionView> views = new ArrayList<>();
        for (HomeSection s : sections.findByActiveTrueOrderBySortOrderAsc()) {
            Map<String, Object> cfg = s.getConfig() == null ? Map.of() : s.getConfig();
            List<BannerView> b = List.of();
            List<ProductCard> p = List.of();
            List<CategoryNode> c = List.of();
            try {
                switch (s.getSectionType()) {
                    case HERO_CAROUSEL, BANNER_STRIP -> b = liveBanners(Banner.Placement.valueOf(
                        String.valueOf(cfg.getOrDefault("placement",
                            s.getSectionType() == HomeSection.Type.HERO_CAROUSEL ? "HOME_HERO" : "HOME_STRIP"))));
                    case PRODUCT_RAIL -> {
                        String cat = (String) cfg.get("categorySlug");
                        String sort = String.valueOf(cfg.getOrDefault("sort", "newest"));
                        int limit = ((Number) cfg.getOrDefault("limit", 8)).intValue();
                        p = catalog.listProducts(new ProductFilter(cat, null, null, null, null, null, null, sort),
                            0, limit).content();
                    }
                    case CATEGORY_GRID -> {
                        if (tree == null) tree = catalog.categoryTree();
                        Object slugs = cfg.get("categorySlugs");
                        Set<String> wanted = slugs instanceof Collection<?> col
                            ? new LinkedHashSet<>(col.stream().map(String::valueOf).toList()) : Set.of();
                        c = tree.stream().filter(n -> wanted.isEmpty() || wanted.contains(n.slug())).toList();
                    }
                    case RICH_TEXT -> { /* config.html rendered (sanitised) client-side */ }
                }
            } catch (RuntimeException ex) {
                // A misconfigured section must never take the homepage down.
                log.warn("Skipping home section {}: {}", s.getId(), ex.getMessage());
                continue;
            }
            views.add(new HomeSectionView(s.getId(), s.getSectionType(), s.getTitle(), cfg, b, p, c));
        }
        return new HomePage(views);
    }

    @Transactional(readOnly = true)
    public List<BannerView> liveBanners(Banner.Placement placement) {
        return banners.findLive(placement, Instant.now()).stream()
            .map(b -> new BannerView(b.getId(), b.getTitle(), b.getSubtitle(), b.getImageUrl(), b.getMobileImageUrl(),
                b.getCtaText(), b.getCtaLink()))
            .toList();
    }

    /** Visible mega-menu tree. HIDDEN items vanish; DISABLED items (or items linking to a non-enabled category) render greyed out. */
    @Transactional(readOnly = true)
    public List<NavNode> navigation() {
        List<NavigationItem> all = navigation.findAllByOrderBySortOrderAscIdAsc();
        return navChildren(all, null);
    }

    private List<NavNode> navChildren(List<NavigationItem> all, Long parentId) {
        List<NavNode> out = new ArrayList<>();
        for (NavigationItem n : all) {
            if (!Objects.equals(n.getParentId(), parentId) || n.getVisibility() == Visibility.HIDDEN) continue;
            Category cat = n.getCategory();
            if (cat != null && cat.getVisibility() == Visibility.HIDDEN) continue;
            boolean disabled = n.getVisibility() == Visibility.DISABLED
                || (cat != null && cat.getVisibility() == Visibility.DISABLED);
            String href = switch (n.getLinkType()) {
                case CATEGORY -> cat == null ? null : "/c/" + cat.getSlug();
                case URL -> n.getUrl();
                case NONE -> null;
            };
            out.add(new NavNode(n.getId(), n.getLabel(), disabled ? null : href, disabled, n.getBadgeText(),
                navChildren(all, n.getId())));
        }
        return out;
    }

    // ================================================================= admin

    @Transactional(readOnly = true)
    public List<BannerAdminView> listBanners() {
        Instant now = Instant.now();
        return banners.findAllByOrderByPlacementAscSortOrderAsc().stream().map(b -> toAdmin(b, now)).toList();
    }

    public BannerAdminView saveBanner(Long id, BannerRequest req) {
        if (req.startsAt() != null && req.endsAt() != null && !req.endsAt().isAfter(req.startsAt())) {
            throw ApiException.badRequest("INVALID_SCHEDULE", "End time must be after start time");
        }
        Banner b = id == null ? new Banner() : banners.findById(id).orElseThrow(() -> ApiException.notFound("Banner", id));
        b.setTitle(req.title());
        b.setSubtitle(req.subtitle());
        b.setImageUrl(req.imageUrl());
        b.setMobileImageUrl(req.mobileImageUrl());
        b.setCtaText(req.ctaText());
        b.setCtaLink(req.ctaLink());
        b.setPlacement(req.placement());
        b.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        b.setActive(req.active() == null || req.active());
        b.setStartsAt(req.startsAt());
        b.setEndsAt(req.endsAt());
        return toAdmin(banners.save(b), Instant.now());
    }

    public void deleteBanner(Long id) {
        banners.deleteById(id);
    }

    private static BannerAdminView toAdmin(Banner b, Instant now) {
        boolean live = b.isActive() && (b.getStartsAt() == null || !b.getStartsAt().isAfter(now))
            && (b.getEndsAt() == null || b.getEndsAt().isAfter(now));
        return new BannerAdminView(b.getId(), b.getTitle(), b.getSubtitle(), b.getImageUrl(), b.getMobileImageUrl(),
            b.getCtaText(), b.getCtaLink(), b.getPlacement(), b.getSortOrder(), b.isActive(), b.getStartsAt(),
            b.getEndsAt(), live);
    }

    @Transactional(readOnly = true)
    public List<HomeSectionAdminView> listSections() {
        return sections.findAllByOrderBySortOrderAsc().stream().map(ContentService::toAdmin).toList();
    }

    public HomeSectionAdminView saveSection(Long id, HomeSectionRequest req) {
        HomeSection s = id == null ? new HomeSection()
            : sections.findById(id).orElseThrow(() -> ApiException.notFound("Home section", id));
        s.setSectionType(req.sectionType());
        s.setTitle(req.title());
        s.setConfig(req.config() == null ? new HashMap<>() : new HashMap<>(req.config()));
        s.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        s.setActive(req.active() == null || req.active());
        return toAdmin(sections.save(s));
    }

    public List<HomeSectionAdminView> reorderSections(List<Long> orderedIds) {
        Map<Long, HomeSection> byId = new HashMap<>();
        sections.findAllById(orderedIds).forEach(s -> byId.put(s.getId(), s));
        for (int i = 0; i < orderedIds.size(); i++) {
            HomeSection s = byId.get(orderedIds.get(i));
            if (s != null) s.setSortOrder(i + 1);
        }
        sections.flush();
        return listSections();
    }

    public void deleteSection(Long id) {
        sections.deleteById(id);
    }

    private static HomeSectionAdminView toAdmin(HomeSection s) {
        return new HomeSectionAdminView(s.getId(), s.getSectionType(), s.getTitle(), s.getConfig(), s.getSortOrder(),
            s.isActive(), s.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public List<NavItemAdminView> listNavigation() {
        return navigation.findAllByOrderBySortOrderAscIdAsc().stream().map(ContentService::toAdmin).toList();
    }

    public NavItemAdminView saveNavItem(Long id, NavItemRequest req) {
        NavigationItem n = id == null ? new NavigationItem()
            : navigation.findById(id).orElseThrow(() -> ApiException.notFound("Navigation item", id));
        if (req.parentId() != null) {
            NavigationItem parent = navigation.findById(req.parentId())
                .orElseThrow(() -> ApiException.notFound("Parent navigation item", req.parentId()));
            if (parent.getParentId() != null) {
                throw ApiException.badRequest("NAV_DEPTH", "The mega menu supports two levels");
            }
            if (Objects.equals(req.parentId(), id)) {
                throw ApiException.badRequest("INVALID_PARENT", "An item cannot be its own parent");
            }
        }
        if (req.linkType() == NavigationItem.LinkType.CATEGORY && req.categoryId() == null) {
            throw ApiException.badRequest("CATEGORY_REQUIRED", "Choose a category for a category link");
        }
        if (req.linkType() == NavigationItem.LinkType.URL && (req.url() == null || req.url().isBlank())) {
            throw ApiException.badRequest("URL_REQUIRED", "Enter a URL for a URL link");
        }
        n.setParentId(req.parentId());
        n.setLabel(req.label());
        n.setLinkType(req.linkType());
        n.setCategory(req.linkType() == NavigationItem.LinkType.CATEGORY
            ? categories.findById(req.categoryId()).orElseThrow(() -> ApiException.notFound("Category", req.categoryId()))
            : null);
        n.setUrl(req.linkType() == NavigationItem.LinkType.URL ? req.url() : null);
        n.setBadgeText(req.badgeText());
        n.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        if (req.visibility() != null) n.setVisibility(req.visibility());
        return toAdmin(navigation.save(n));
    }

    public NavItemAdminView setNavVisibility(Long id, Visibility visibility) {
        NavigationItem n = navigation.findById(id).orElseThrow(() -> ApiException.notFound("Navigation item", id));
        n.setVisibility(visibility);
        return toAdmin(n);
    }

    public void deleteNavItem(Long id) {
        navigation.deleteById(id);   // children cascade in the database
    }

    private static NavItemAdminView toAdmin(NavigationItem n) {
        return new NavItemAdminView(n.getId(), n.getParentId(), n.getLabel(), n.getLinkType(),
            n.getCategory() == null ? null : n.getCategory().getId(),
            n.getCategory() == null ? null : n.getCategory().getName(), n.getUrl(), n.getBadgeText(), n.getSortOrder(),
            n.getVisibility());
    }
}
