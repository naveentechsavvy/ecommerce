package com.apparel.commerce.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless OAuth2 resource server. Keycloak issues the tokens; realm roles
 * (CUSTOMER, ADMIN, OPERATIONS) become Spring authorities ROLE_*.
 * Storefront endpoints accept an optional bearer token, so guest and
 * logged-in shoppers share the same checkout and coupon APIs.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(c -> {})
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**", "/api/v1/content/**").permitAll()
                .requestMatchers("/api/v1/coupons/validate", "/api/v1/checkout/**", "/api/v1/orders/track",
                                 "/api/v1/chat").permitAll()
                .requestMatchers("/api/v1/me/**").authenticated()
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "OPERATIONS")
                .anyRequest().denyAll())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(OAuth2ResourceServerProperties props, AppProperties app) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(props.getJwt().getJwkSetUri()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(app.security().issuer()));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(SecurityConfig::realmRoles);
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    static Collection<GrantedAuthority> realmRoles(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> roles) {
            for (Object role : roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toString().toUpperCase()));
            }
        }
        return authorities;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(AppProperties app) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(app.cors().allowedOrigins());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
