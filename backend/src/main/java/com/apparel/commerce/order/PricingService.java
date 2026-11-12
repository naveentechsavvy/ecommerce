package com.apparel.commerce.order;

import com.apparel.commerce.catalog.Product;
import com.apparel.commerce.catalog.ProductVariant;
import com.apparel.commerce.catalog.ProductVariantRepository;
import com.apparel.commerce.common.CurrentUser;
import com.apparel.commerce.config.AppProperties;
import com.apparel.commerce.coupon.CouponService;
import com.apparel.commerce.order.OrderDtos.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Server-authoritative cart pricing. The client only ever sends variant ids
 * and quantities; prices, stock, discounts and shipping are always computed here.
 * Prices are GST-inclusive (Indian B2C convention) so tax is reported as 0 extra.
 */
@Service
@RequiredArgsConstructor
public class PricingService {

    private final ProductVariantRepository variants;
    private final CouponService coupons;
    private final AppProperties props;

    @Transactional(readOnly = true)
    public Quote quote(List<CartLine> requested, String couponCode, Optional<CurrentUser> user, String email) {
        Map<Long, Integer> qty = new LinkedHashMap<>();
        for (CartLine l : requested) qty.merge(l.variantId(), l.quantity(), Integer::sum);

        Map<Long, ProductVariant> byId = variants.findAllWithProductByIdIn(qty.keySet()).stream()
            .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        List<QuoteLine> lines = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (var e : qty.entrySet()) {
            ProductVariant v = byId.get(e.getKey());
            if (v == null) {
                issues.add("An item in your bag is no longer available.");
                continue;
            }
            Product p = v.getProduct();
            int q = Math.min(e.getValue(), 10);
            BigDecimal unit = v.effectivePrice();
            BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(q));
            String issue = null;
            if (p.getStatus() != Product.Status.ACTIVE || !v.isActive()) {
                issue = "No longer available";
            } else if (v.available() <= 0) {
                issue = "Out of stock";
            } else if (v.available() < q) {
                issue = "Only " + v.available() + " left";
            }
            if (issue != null) issues.add(p.getName() + " (" + v.getSize() + ", " + v.getColor() + "): " + issue);
            String image = p.getImages().stream()
                .filter(i -> i.getColor() != null && i.getColor().equalsIgnoreCase(v.getColor()))
                .map(com.apparel.commerce.catalog.ProductImage::getUrl).findFirst().orElse(p.primaryImageUrl());
            lines.add(new QuoteLine(v.getId(), v.getSku(), p.getName(), p.getSlug(), v.getSize(), v.getColor(),
                image, unit, q, lineTotal, v.available(), issue));
            subtotal = subtotal.add(lineTotal);
        }

        CouponService.Evaluation coupon = coupons.evaluate(couponCode, subtotal, user,
            user.map(CurrentUser::email).orElse(email));
        BigDecimal discount = coupon.applied() ? coupon.discount() : BigDecimal.ZERO;
        BigDecimal afterDiscount = subtotal.subtract(discount);
        BigDecimal shipping = subtotal.signum() == 0 || afterDiscount.compareTo(props.checkout().freeShippingThreshold()) >= 0
            ? BigDecimal.ZERO : props.checkout().flatShippingFee();
        BigDecimal total = afterDiscount.add(shipping).setScale(2, RoundingMode.HALF_UP);

        return new Quote(lines, scale(subtotal), scale(discount), scale(shipping), scale(BigDecimal.ZERO), total,
            props.checkout().currency(), coupon, issues);
    }

    private static BigDecimal scale(BigDecimal b) {
        return b.setScale(2, RoundingMode.HALF_UP);
    }
}
