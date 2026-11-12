package com.apparel.commerce.inventory;

import com.apparel.commerce.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

/** Real-time stock adjustments - available to ADMIN and OPERATIONS. */
@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final InventoryService inventory;

    @GetMapping
    public PageResponse<InventoryService.InventoryRow> list(@RequestParam(required = false) String q,
                                                            @RequestParam(defaultValue = "false") boolean lowStockOnly,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "50") int size) {
        return inventory.search(q, lowStockOnly, PageRequest.of(page, Math.min(size, 200), Sort.by("variantId")));
    }

    @PostMapping("/{variantId}/adjustments")
    public InventoryService.InventoryRow adjust(@PathVariable Long variantId,
                                                @Valid @RequestBody InventoryService.AdjustmentRequest req) {
        return inventory.adjust(variantId, req);
    }

    @GetMapping("/{variantId}/movements")
    public PageResponse<InventoryService.MovementRow> movements(@PathVariable Long variantId,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        return inventory.movements(variantId, PageRequest.of(page, Math.min(size, 100)));
    }
}
