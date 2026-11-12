package com.apparel.commerce.content;

import com.apparel.commerce.content.ContentDtos.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/** Public, cacheable CMS reads for the storefront. */
@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class ContentController {

    private static final CacheControl SHORT_CACHE = CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic();

    private final ContentService content;

    @GetMapping("/home")
    public ResponseEntity<HomePage> home() {
        return ResponseEntity.ok().cacheControl(SHORT_CACHE).body(content.homePage());
    }

    @GetMapping("/navigation")
    public ResponseEntity<List<NavNode>> navigation() {
        return ResponseEntity.ok().cacheControl(SHORT_CACHE).body(content.navigation());
    }

    @GetMapping("/banners")
    public ResponseEntity<List<BannerView>> banners(@RequestParam Banner.Placement placement) {
        return ResponseEntity.ok().cacheControl(SHORT_CACHE).body(content.liveBanners(placement));
    }
}
