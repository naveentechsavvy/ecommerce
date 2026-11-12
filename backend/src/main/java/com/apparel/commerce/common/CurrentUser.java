package com.apparel.commerce.common;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Resolved caller identity; {@code customerId} is the Keycloak subject. Empty for guests. */
public record CurrentUser(String customerId, String email, String name) {

    public static Optional<CurrentUser> get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            return Optional.of(new CurrentUser(
                jwt.getToken().getSubject(),
                jwt.getToken().getClaimAsString("email"),
                jwt.getToken().getClaimAsString("name")));
        }
        return Optional.empty();
    }

    public static CurrentUser require() {
        return get().orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Login required"));
    }

    public static String actorName() {
        return get().map(u -> u.email() != null ? u.email() : u.customerId()).orElse("system");
    }
}
