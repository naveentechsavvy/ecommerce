package com.apparel.commerce.chat;

import com.apparel.commerce.catalog.CatalogDtos.ProductCard;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class ChatDtos {
    private ChatDtos() {}

    public record ChatMessage(@NotBlank @Pattern(regexp = "user|assistant") String role,
                              @NotBlank @Size(max = 2000) String content) {}

    /** Stateless: the widget sends the running transcript each turn (trimmed server-side). */
    public record ChatRequest(@NotEmpty @Size(max = 40) List<@Valid ChatMessage> messages) {}

    /** {@code mode} is "ai" when Claude answered, "offline" for the keyword fallback. */
    public record ChatResponse(String reply, List<ProductCard> products, String mode) {}
}
