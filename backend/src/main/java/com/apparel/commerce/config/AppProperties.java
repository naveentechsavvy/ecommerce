package com.apparel.commerce.config;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Security security, Cors cors, Checkout checkout, Chat chat) {

    public record Security(String issuer) {}

    public record Cors(List<String> allowedOrigins) {}

    public record Checkout(String currency, BigDecimal freeShippingThreshold, BigDecimal flatShippingFee) {}

    public record Chat(String anthropicApiKey, String model, int maxToolIterations, int maxHistoryMessages) {
        public boolean enabled() {
            return anthropicApiKey != null && !anthropicApiKey.isBlank();
        }
    }
}
