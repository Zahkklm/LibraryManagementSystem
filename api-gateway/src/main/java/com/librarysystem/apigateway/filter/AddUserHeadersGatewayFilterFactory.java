package com.librarysystem.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class AddUserHeadersGatewayFilterFactory extends AbstractGatewayFilterFactory<AddUserHeadersGatewayFilterFactory.Config> {

    public AddUserHeadersGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        System.out.println("Gateway filter working: " + config.toString());
        return (exchange, chain) -> {
            // Check if the request path matches a public endpoint
            String path = exchange.getRequest().getPath().toString();
            String method = exchange.getRequest().getMethod().name();
            if (path.equals("/api/users") && method.equals("POST")) {
                // Skip JWT processing for public endpoints
                System.out.println("Skipping JWT processing for public endpoint: " + path);
                return chain.filter(exchange);
            }

            // Process JWT for other endpoints
            return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .flatMap(authentication -> {
                    ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
                    Jwt jwt = authentication.getToken();

                    // Add X-User-Id header (using 'sub' claim as user ID)
                    String userId = jwt.getSubject();
                    if (userId != null) {
                        builder.header("X-User-Id", userId);
                    }

                    // Add X-User-Roles header (comma-separated roles, without "ROLE_" prefix)
                    String roles = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                        .collect(Collectors.joining(","));
                    if (!roles.isEmpty()) {
                        builder.header("X-User-Roles", roles);
                    }

                    // Add X-User-Email header (if email is in a claim like 'email')
                    String email = jwt.getClaimAsString("email");
                    if (email != null) {
                        builder.header("X-User-Email", email);
                    }

                    return chain.filter(exchange.mutate().request(builder.build()).build());
                })
                .switchIfEmpty(chain.filter(exchange)); // If no principal, proceed without adding headers
        };
    }

    public static class Config {
        // Configuration properties for the filter 
    }
}