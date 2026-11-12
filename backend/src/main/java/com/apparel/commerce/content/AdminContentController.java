package com.apparel.commerce.content;

import com.apparel.commerce.catalog.CatalogDtos.VisibilityRequest;
import com.apparel.commerce.content.ContentDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** CMS management - ADMIN only. */
@RestController
@RequestMapping("/api/v1/admin/content")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminContentController {

    private final ContentService content;

    // banners
    @GetMapping("/banners")
    public List<BannerAdminView> banners() {
        return content.listBanners();
    }

    @PostMapping("/banners")
    @ResponseStatus(HttpStatus.CREATED)
    public BannerAdminView createBanner(@Valid @RequestBody BannerRequest req) {
        return content.saveBanner(null, req);
    }

    @PutMapping("/banners/{id}")
    public BannerAdminView updateBanner(@PathVariable Long id, @Valid @RequestBody BannerRequest req) {
        return content.saveBanner(id, req);
    }

    @DeleteMapping("/banners/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBanner(@PathVariable Long id) {
        content.deleteBanner(id);
    }

    // homepage layout
    @GetMapping("/home-sections")
    public List<HomeSectionAdminView> sections() {
        return content.listSections();
    }

    @PostMapping("/home-sections")
    @ResponseStatus(HttpStatus.CREATED)
    public HomeSectionAdminView createSection(@Valid @RequestBody HomeSectionRequest req) {
        return content.saveSection(null, req);
    }

    @PutMapping("/home-sections/{id}")
    public HomeSectionAdminView updateSection(@PathVariable Long id, @Valid @RequestBody HomeSectionRequest req) {
        return content.saveSection(id, req);
    }

    @PutMapping("/home-sections/order")
    public List<HomeSectionAdminView> reorder(@Valid @RequestBody ReorderRequest req) {
        return content.reorderSections(req.orderedIds());
    }

    @DeleteMapping("/home-sections/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSection(@PathVariable Long id) {
        content.deleteSection(id);
    }

    // mega menu
    @GetMapping("/navigation")
    public List<NavItemAdminView> navigation() {
        return content.listNavigation();
    }

    @PostMapping("/navigation")
    @ResponseStatus(HttpStatus.CREATED)
    public NavItemAdminView createNav(@Valid @RequestBody NavItemRequest req) {
        return content.saveNavItem(null, req);
    }

    @PutMapping("/navigation/{id}")
    public NavItemAdminView updateNav(@PathVariable Long id, @Valid @RequestBody NavItemRequest req) {
        return content.saveNavItem(id, req);
    }

    @PatchMapping("/navigation/{id}/visibility")
    public NavItemAdminView navVisibility(@PathVariable Long id, @Valid @RequestBody VisibilityRequest req) {
        return content.setNavVisibility(id, req.visibility());
    }

    @DeleteMapping("/navigation/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNav(@PathVariable Long id) {
        content.deleteNavItem(id);
    }
}
