package com.apparel.commerce.chat;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import java.util.List;
import java.util.Map;

/** JSON-schema tool definitions exposed to Claude. Kept static and ordered so the prompt prefix stays cacheable. */
final class ToolCatalog {
    private ToolCatalog() {}

    static final String SEARCH_PRODUCTS = "search_products";
    static final String PRODUCT_DETAILS = "get_product_details";
    static final String RECOMMEND_SIZE = "recommend_size";
    static final String ORDER_STATUS = "get_order_status";
    static final String LIST_CATEGORIES = "list_categories";

    static List<Tool> all() {
        return List.of(
            tool(SEARCH_PRODUCTS,
                "Search the live catalog. Use for any product discovery request (e.g. 'black tee under 1000', "
                    + "'summer dress'). Returns up to 6 products with price in INR, colours and stock status. "
                    + "Prefer short keyword queries; use category slugs from list_categories when the shopper names a department.",
                Map.of(
                    "query", prop("string", "Keywords describing the item, e.g. 'linen shirt' or 'wide leg jeans'"),
                    "category", prop("string", "Optional category slug, e.g. 'men', 'women-dresses'"),
                    "max_price", prop("number", "Optional maximum price in INR"),
                    "size", prop("string", "Optional size filter, e.g. 'M' or '32'"),
                    "color", prop("string", "Optional colour filter, e.g. 'Black'"),
                    "in_stock_only", prop("boolean", "Only return items with stock (default true)")),
                List.of("query")),
            tool(PRODUCT_DETAILS,
                "Get full details for one product by slug: description, material, care, per-colour/size "
                    + "availability and the size chart. Use before answering questions about a specific item.",
                Map.of("slug", prop("string", "Product slug from search results")),
                List.of("slug")),
            tool(RECOMMEND_SIZE,
                "Recommend a size for a product from the shopper's body measurements using the product's size chart. "
                    + "Provide whichever measurements the shopper gave (cm). Always use this rather than guessing.",
                Map.of(
                    "slug", prop("string", "Product slug"),
                    "height_cm", prop("number", "Height in cm"),
                    "chest_cm", prop("number", "Chest circumference in cm (menswear tops)"),
                    "bust_cm", prop("number", "Bust circumference in cm (womenswear)"),
                    "waist_cm", prop("number", "Waist circumference in cm (bottoms)"),
                    "fit_preference", Map.of("type", "string", "enum", List.of("snug", "regular", "relaxed"),
                        "description", "How the shopper likes clothes to fit")),
                List.of("slug")),
            tool(ORDER_STATUS,
                "Look up the status of an order. Signed-in shoppers: order number only. Guests: order number AND the "
                    + "email used at checkout are both required - ask for any that is missing before calling.",
                Map.of(
                    "order_number", prop("string", "Order number, e.g. AC250101-ABC123"),
                    "email", prop("string", "Checkout email (required for guests)")),
                List.of("order_number")),
            tool(LIST_CATEGORIES,
                "List store departments and sub-categories with their slugs.",
                Map.of(), List.of()));
    }

    private static Map<String, Object> prop(String type, String description) {
        return Map.of("type", type, "description", description);
    }

    private static Tool tool(String name, String description, Map<String, Object> properties, List<String> required) {
        Tool.InputSchema.Properties.Builder props = Tool.InputSchema.Properties.builder();
        // sorted for a byte-stable request prefix (prompt caching)
        properties.entrySet().stream().sorted(Map.Entry.comparingByKey())
            .forEach(e -> props.putAdditionalProperty(e.getKey(), JsonValue.from(e.getValue())));
        return Tool.builder()
            .name(name)
            .description(description)
            .inputSchema(Tool.InputSchema.builder().properties(props.build()).required(required).build())
            .build();
    }
}
