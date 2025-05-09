package com.librarysystem.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private static final Logger logger = LoggerFactory.getLogger(AddUserHeadersGatewayFilterFactory.class);

    @Value("${spring.cloud.gateway.secret}")
    private String gatewaySecret;

    public AddUserHeadersGatewayFilterFactory() {
        super(Config.class);
        logger.debug("AddUserHeadersGatewayFilterFactory initialized");
    }

    @Override
    public GatewayFilter apply(Config config) {
        logger.debug("Creating AddUserHeaders filter");
        return (exchange, chain) -> {
            // Check if the request path matches a public endpoint
            String path = exchange.getRequest().getPath().toString();
            String method = exchange.getRequest().getMethod().name();
            
            logger.debug("Processing request: {} {}", method, path);
            
            if (path.equals("/api/users") && method.equals("POST")) {
                // Skip JWT processing for public endpoints
                logger.info("Skipping JWT processing for public endpoint: {} {}", method, path);
                return chain.filter(exchange);
            }

            // Process JWT for other endpoints
            logger.debug("Attempting to process JWT for endpoint: {} {}", method, path);
            return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .flatMap(authentication -> {
                    ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
                    Jwt jwt = authentication.getToken();
                    logger.debug("JWT found in request for path: {}", path);

                    // Add X-User-Id header (using 'sub' claim as user ID)
                    String userId = jwt.getSubject();
                    if (userId != null) {
                        builder.header("X-User-Id", userId);
                        logger.debug("Added X-User-Id header: {}", userId);
                    } else {
                        logger.warn("JWT missing subject claim, X-User-Id header not added");
                    }

                    // Add X-User-Roles header (comma-separated roles, without "ROLE_" prefix)
                    String roles = authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                        .collect(Collectors.joining(","));
                    
                    if (!roles.isEmpty()) {
                        builder.header("X-User-Roles", roles);
                        logger.debug("Added X-User-Roles header: {}", roles);
                    } else {
                        logger.warn("JWT contains no roles, X-User-Roles header not added");
                    }

                    // Add X-User-Email header (if email is in a claim like 'email')
                    String email = jwt.getClaimAsString("email");
                    if (email != null) {
                        builder.header("X-User-Email", email);
                        logger.debug("Added X-User-Email header: {}", email);
                    } else {
                        logger.debug("JWT missing email claim, X-User-Email header not added");
                    }
                    
                    // Add X-Gateway-Secret header for internal service authentication
                    // This would normally come from configuration
                    builder.header("X-Gateway-Secret", gatewaySecret);
                    logger.debug("Added X-Gateway-Secret header for downstream service authentication");
               
                    logger.info("User context headers added for request: {} {}, user: {}, roles: {}", 
                               method, path, userId, roles);
                    return chain.filter(exchange.mutate().request(builder.build()).build());
                })
                .switchIfEmpty(chain.filter(exchange)
                    .doOnSuccess(v -> logger.info("Request processed without JWT: {} {}", method, path))
                    .doOnError(e -> logger.error("Error processing request: {} {}, error: {}", method, path, e.getMessage()))
                );
        };
    }

    public static class Config {
        // Configuration properties for the filter 
    }
}
