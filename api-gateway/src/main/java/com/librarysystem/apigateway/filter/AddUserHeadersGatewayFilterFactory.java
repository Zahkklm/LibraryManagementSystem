package com.librarysystem.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Component
public class AddUserHeadersGatewayFilterFactory extends AbstractGatewayFilterFactory<AddUserHeadersGatewayFilterFactory.Config> {

    public AddUserHeadersGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> exchange.getPrincipal()
            .cast(JwtAuthenticationToken.class) // Assuming you use JwtAuthenticationToken after JWT validation
            .flatMap(authentication -> {
                ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
                Jwt jwt = authentication.getToken();

                // Add X-User-Id header (using 'sub' claim as user ID)
                String userId = jwt.getSubject(); // 'sub' claim is often the user ID
                if (userId != null) {
                    builder.header("X-User-Id", userId);
                }

                // Add X-User-Roles header (comma-separated roles, without "ROLE_" prefix)
                String roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role) // Remove "ROLE_" prefix
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
    }

    public static class Config {
        // TODO: Add configuration properties for the filter
    }
}