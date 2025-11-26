package com.example.bookingservice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(authz -> authz
                        // FIXED: Allow all actuator health endpoints without authentication
                        // This is critical for Kubernetes liveness/readiness probes
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",  // CRITICAL: Added for /actuator/health/liveness and /actuator/health/readiness
                                "/actuator/info"
                        ).permitAll()
                        // Other public endpoints
                        .requestMatchers(
                                "/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api/bookings/confirm/**"
                        ).permitAll()
                        // All booking endpoints require authentication
                        .requestMatchers("/api/bookings/**").authenticated()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        converter.setPrincipalClaimName("preferred_username");
        return converter;
    }

    // CRITICAL: NOT a @Bean - private method only
    private Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            // Extract roles from realm_access claim
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) realmAccess.get("roles");

                authorities = roles.stream()
                        .map(this::mapRoleToAuthority)
                        .collect(Collectors.toList());
            }

            return authorities;
        };
    }

    private GrantedAuthority mapRoleToAuthority(String role) {
        // If role already starts with "ROLE_", use it as-is
        if (role.startsWith("ROLE_")) {
            return new SimpleGrantedAuthority(role);
        }

        // Standard Keycloak internal roles - use as-is
        if (role.startsWith("default-roles-") ||
                role.equals("offline_access") ||
                role.equals("uma_authorization")) {
            return new SimpleGrantedAuthority(role);
        }

        // Custom application roles - add ROLE_ prefix
        return new SimpleGrantedAuthority("ROLE_" + role);
    }
}