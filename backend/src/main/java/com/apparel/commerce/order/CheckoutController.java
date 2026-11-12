package com.apparel.commerce.order;

import com.apparel.commerce.coupon.CouponService;
import com.apparel.commerce.order.OrderDtos.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/** Storefront checkout. Works for guests and, when a bearer token is present, for signed-in customers. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class CheckoutController {

    private final CheckoutService checkout;
    private final OrderService orders;

    @PostMapping("/checkout/quote")
    public Quote quote(@Valid @RequestBody QuoteRequest req) {
        return checkout.quote(req);
    }

    @PostMapping("/coupons/validate")
    public CouponService.Evaluation validateCoupon(@Valid @RequestBody CouponValidateRequest req) {
        return checkout.validateCoupon(req);
    }

    @PostMapping("/checkout/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView placeOrder(@Valid @RequestBody PlaceOrderRequest req) {
        return checkout.placeOrder(req);
    }

    @GetMapping("/orders/track")
    public OrderView track(@RequestParam @NotBlank String orderNumber, @RequestParam @NotBlank @Email String email) {
        return orders.track(orderNumber, email);
    }
}
