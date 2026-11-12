package com.apparel.commerce;

import static org.assertj.core.api.Assertions.assertThat;

import com.apparel.commerce.chat.ChatService;
import com.apparel.commerce.order.CheckoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Boots the full Spring context against in-memory H2 (schema from entities, Flyway off).
 * Catches wiring mistakes and invalid JPQL in @Query methods, which Spring Data
 * validates at startup. Postgres-specific behaviour (FTS, conditional stock updates)
 * belongs in Testcontainers integration tests.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:smoke;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=ROWS,VALUE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.chat.anthropic-api-key="
})
class ContextSmokeTest {

    @Autowired
    CheckoutService checkout;

    @Autowired
    ChatService chat;

    @Test
    void contextLoads() {
        assertThat(checkout).isNotNull();
        assertThat(chat).isNotNull();
    }
}
