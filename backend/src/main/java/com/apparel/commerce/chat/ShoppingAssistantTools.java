package com.apparel.commerce.chat;

import com.apparel.commerce.catalog.CatalogDtos.*;
import com.apparel.commerce.catalog.CatalogService;
import com.apparel.commerce.common.ApiException;
import com.apparel.commerce.common.CurrentUser;
import com.apparel.commerce.order.Order;
import com.apparel.commerce.order.OrderService;
import com.apparel.commerce.order.OrderStatusHistory;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The assistant's "retrieval" layer. Instead of a static vector index of the
 * catalog, Claude calls these tools, which query PostgreSQL live - so prices,
 * stock and order status are always current and access-controlled.
 * Every method returns plain maps/lists that are serialised to JSON for the model.
 */
@Component
@RequiredArgsConstructor
public class ShoppingAssistantTools {

    private final CatalogService catalog;
    private final OrderService orders;

    /** Products surfaced during this conversation turn, shown as cards under the reply. */
    public static final class TurnContext {
        final Map<String, ProductCard> surfaced = new LinkedHashMap<>();
    }

    @Transactional(readOnly = true)
    public Object searchProducts(Map<String, Object> in, TurnContext ctx) {
        String query = str(in.get("query"));
        String category = str(in.get("category"));
        BigDecimal maxPrice = in.get("max_price") instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : null;
        List<String> sizes = str(in.get("size")) == null ? null : List.of(str(in.get("size")));
        List<String> colors = str(in.get("color")) == null ? null : List.of(capitalize(str(in.get("color"))));
        Boolean inStock = in.get("in_stock_only") instanceof Boolean b ? b : Boolean.TRUE;

        PageFallback result = search(new ProductFilter(category, query, null, maxPrice, sizes, colors, inStock, "popularity"));
        List<Map<String, Object>> items = new ArrayList<>();
        for (ProductCard c : result.cards) {
            ctx.surfaced.putIfAbsent(c.slug(), c);
            items.add(ordered(
                "name", c.name(), "slug", c.slug(), "url", "/p/" + c.slug(), "brand", c.brand(),
                "price_inr", c.price(), "was_price_inr", c.compareAtPrice(), "colors", c.colors(),
                "in_stock", c.inStock(), "low_stock", c.lowStock(), "badge", c.badge()));
        }
        return ordered("results", items, "count", items.size(), "note", result.note);
    }

    private record PageFallback(List<ProductCard> cards, String note) {}

    private PageFallback search(ProductFilter f) {
        try {
            return new PageFallback(catalog.listProducts(f, 0, 6).content(), null);
        } catch (ApiException e) {
            // unknown category slug -> retry without it rather than failing the tool call
            ProductFilter relaxed = new ProductFilter(null, f.q(), f.minPrice(), f.maxPrice(), f.sizes(), f.colors(), f.inStock(), f.sort());
            return new PageFallback(catalog.listProducts(relaxed, 0, 6).content(),
                "Category '" + f.category() + "' not found; searched all categories.");
        }
    }

    @Transactional(readOnly = true)
    public Object productDetails(Map<String, Object> in, TurnContext ctx) {
        ProductDetail p = catalog.product(Objects.requireNonNull(str(in.get("slug")), "slug is required"));
        Map<String, List<Map<String, Object>>> byColour = new LinkedHashMap<>();
        for (VariantView v : p.variants()) {
            byColour.computeIfAbsent(v.color(), k -> new ArrayList<>())
                .add(ordered("size", v.size(), "price_inr", v.price(), "stock", v.stockStatus()));
        }
        return ordered(
            "name", p.name(), "url", "/p/" + p.slug(), "brand", p.brand(), "price_inr", p.price(),
            "was_price_inr", p.compareAtPrice(), "description", p.description(), "material", p.material(),
            "care", p.careInstructions(), "in_stock", p.inStock(), "variants_by_color", byColour,
            "size_chart", p.sizeChart() == null ? null
                : ordered("rows", p.sizeChart().rows(), "fit_notes", p.sizeChart().fitNotes()));
    }

    /**
     * Deterministic size recommendation from the product's size chart. The model
     * explains the result but does not do the arithmetic itself.
     */
    @Transactional(readOnly = true)
    public Object recommendSize(Map<String, Object> in, TurnContext ctx) {
        ProductDetail p = catalog.product(Objects.requireNonNull(str(in.get("slug")), "slug is required"));
        if (p.sizeChart() == null || p.sizeChart().rows() == null || p.sizeChart().rows().isEmpty()) {
            return Map.of("error", "This product has no size chart; suggest the customer's usual size.");
        }
        Map<String, Double> measurements = new LinkedHashMap<>();
        for (String key : List.of("chest_cm", "bust_cm", "waist_cm", "height_cm")) {
            if (in.get(key) instanceof Number n) measurements.put(toChartKey(key), n.doubleValue());
        }
        if (measurements.isEmpty()) {
            return Map.of("error", "Ask the customer for at least one measurement: chest, bust, waist or height in cm.");
        }
        List<Map<String, Object>> rows = p.sizeChart().rows();
        String best = null;
        double bestScore = Double.MAX_VALUE;
        String usedKey = null;
        for (Map<String, Object> row : rows) {
            double score = 0;
            int matched = 0;
            for (var m : measurements.entrySet()) {
                if (row.get(m.getKey()) instanceof List<?> range && range.size() == 2) {
                    double lo = ((Number) range.get(0)).doubleValue();
                    double hi = ((Number) range.get(1)).doubleValue();
                    double weight = m.getKey().equals("heightCm") ? 0.5 : 1.0;   // girth matters more than height
                    score += weight * (m.getValue() < lo ? lo - m.getValue() : m.getValue() > hi ? m.getValue() - hi : 0);
                    matched++;
                    usedKey = m.getKey();
                }
            }
            if (matched > 0 && score < bestScore) {
                bestScore = score;
                best = String.valueOf(row.get("size"));
            }
        }
        if (best == null) {
            return Map.of("error", "The size chart doesn't use the measurements provided. Chart columns: " + rows.get(0).keySet());
        }
        List<String> order = rows.stream().map(r -> String.valueOf(r.get("size"))).toList();
        int idx = order.indexOf(best);
        String fit = str(in.get("fit_preference"));
        String adjusted = best;
        if ("relaxed".equalsIgnoreCase(fit) && idx < order.size() - 1) adjusted = order.get(idx + 1);
        if ("snug".equalsIgnoreCase(fit) && bestScore > 0 && idx > 0) adjusted = order.get(idx - 1);
        final String size = adjusted;
        List<String> coloursInStock = p.variants().stream()
            .filter(v -> v.size().equals(size) && !"OUT_OF_STOCK".equals(v.stockStatus()))
            .map(VariantView::color).distinct().toList();
        return ordered(
            "recommended_size", size, "chart_best_match", best, "exact_fit", bestScore == 0,
            "basis", usedKey, "fit_notes", p.sizeChart().fitNotes(),
            "colors_in_stock_in_size", coloursInStock);
    }

    /** Signed-in shoppers can only see their own orders; guests must supply the checkout email. */
    @Transactional(readOnly = true)
    public Object orderStatus(Map<String, Object> in, TurnContext ctx) {
        String number = str(in.get("order_number"));
        if (number == null) return Map.of("error", "Ask the customer for their order number (e.g. AC250101-ABC123).");
        Optional<CurrentUser> user = CurrentUser.get();
        Optional<Order> order = user.isPresent()
            ? orders.lookupForCustomer(number, user.get().customerId())
            : orders.lookup(number, str(in.get("email")));
        if (order.isEmpty()) {
            return Map.of("found", false, "hint", user.isPresent()
                ? "No order with that number on this account."
                : "No order matches that number and email. Guests must give the email used at checkout.");
        }
        Order o = order.get();
        List<OrderStatusHistory> history = o.getHistory();
        OrderStatusHistory last = history.isEmpty() ? null : history.get(history.size() - 1);
        return ordered(
            "found", true, "order_number", o.getOrderNumber(), "status", o.getStatus().name(),
            "placed_at", o.getCreatedAt().toString(), "total_inr", o.getGrandTotal(),
            "items", o.getItems().stream().map(i -> i.getProductName() + " (" + i.getSize() + ", " + i.getColor() + ") x" + i.getQuantity()).toList(),
            "tracking_number", o.getTrackingNumber(), "ship_to_city", o.getShippingAddress().getCity(),
            "last_update", last == null ? null : ordered("status", last.getToStatus().name(), "note", last.getNote(), "at", last.getChangedAt().toString()),
            "self_service_cancel_allowed", o.getStatus().name().equals("PAID") && user.isPresent());
    }

    @Transactional(readOnly = true)
    public Object listCategories(Map<String, Object> in, TurnContext ctx) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (CategoryNode n : catalog.categoryTree()) {
            out.add(ordered("name", n.name(), "slug", n.slug(), "available", n.visibility().name().equals("ENABLED"),
                "subcategories", n.children().stream().map(c -> Map.of("name", c.name(), "slug", c.slug())).toList()));
        }
        return Map.of("categories", out);
    }

    // ----------------------------------------------------------------- utils

    private static String toChartKey(String toolKey) {
        return switch (toolKey) {
            case "chest_cm" -> "chestCm";
            case "bust_cm" -> "bustCm";
            case "waist_cm" -> "waistCm";
            default -> "heightCm";
        };
    }

    static String str(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1).toLowerCase(Locale.ROOT);
    }

    /** LinkedHashMap that tolerates null values (Map.of does not). */
    static Map<String, Object> ordered(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            if (kv[i + 1] != null) m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }
}
