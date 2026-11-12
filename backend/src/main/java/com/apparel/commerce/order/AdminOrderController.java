package com.apparel.commerce.order;

import com.apparel.commerce.common.PageResponse;
import com.apparel.commerce.order.OrderDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Order tracking & fulfilment - ADMIN and OPERATIONS. */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orders;

    @GetMapping("/orders")
    public PageResponse<OrderSummary> list(@RequestParam(required = false) OrderStatus status,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "25") int size) {
        return orders.search(status, q, page, size);
    }

    @GetMapping("/orders/{orderNumber}")
    public OrderView get(@PathVariable String orderNumber) {
        return orders.adminGet(orderNumber);
    }

    @PatchMapping("/orders/{orderNumber}/status")
    public OrderView changeStatus(@PathVariable String orderNumber, @Valid @RequestBody StatusChangeRequest req) {
        return orders.changeStatus(orderNumber, req);
    }

    @GetMapping("/dashboard")
    public DashboardSummary dashboard() {
        return orders.dashboard();
    }
}
