package com.librarysystem.apigateway.config;

import jakarta.ws.rs.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Security configuration for API Gateway.
 * <p>
 * This class configures:
 * - OAuth2 Resource Server with JWT validation
 * - Role-based access control for API endpoints
 * - CORS policies
 * - Method-level security annotations
 * <p>
 * Authentication is delegated to Keycloak.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Value("${cors.allowed-origins:*}")
    private List<String> allowedOrigins;

    /**
     * Configures the security filter chain.
     *
     * @param http The ServerHttpSecurity to configure
     * @return The configured SecurityWebFilterChain
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchanges -> exchanges
                        // Allow public access to auth endpoints
                        .pathMatchers(HttpMethod.POST, "/api/auth/login").permitAll() 
                        .pathMatchers("/api/auth/**").permitAll() 
                        
                        // Allow public access to user registration
                        .pathMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/users/validate").permitAll()
                        
                        // Secure other user endpoints
                        .pathMatchers("/api/users/**").hasAnyRole("ADMIN", "LIBRARIAN")
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor())))
                .build();
    }

    /**
     * Creates a converter that extracts authorities from the Keycloak JWT token.
     * <p>
     * This looks for realm_access.roles claim in the JWT and converts it to Spring Security authorities.
     *
     * @return A reactive converter for JWT authentication
     */
    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
        JwtAuthenticationConverter jwtAuthConverter = new JwtAuthenticationConverter();
        jwtAuthConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Extract standard claims from token
            JwtGrantedAuthoritiesConverter standardConverter = new JwtGrantedAuthoritiesConverter();
            Collection<GrantedAuthority> standardAuthorities = standardConverter.convert(jwt);
            
            // Extract Keycloak realm roles
            // Keycloak puts roles in realm_access.roles
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return standardAuthorities;
            }
            
            @SuppressWarnings("unchecked")
            Collection<String> roles = (Collection<String>) realmAccess.get("roles");
            
            // Convert roles to authorities with ROLE_ prefix
            Collection<GrantedAuthority> keycloakAuthorities = roles.stream()
                    .map(role -> "ROLE_" + role.toUpperCase())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            
            // Combine standard and Keycloak authorities
            if (standardAuthorities != null) {
                keycloakAuthorities.addAll(standardAuthorities);
            }
            
            return keycloakAuthorities;
        });
        
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthConverter);
    }
    
    /**
     * Configures CORS settings.
     *
     * @return CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        allowedOrigins = List.of("*");

        // Configure allowed origins (from application properties)
        configuration.setAllowedOrigins(allowedOrigins);
        
        // Allow common HTTP methods
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        
        // Allow common headers
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", 
                "Content-Type", 
                "X-Requested-With", 
                "Accept", 
                "Origin", 
                "Access-Control-Request-Method", 
                "Access-Control-Request-Headers"
        ));
        
        // Allow credentials (cookies, auth headers)
        configuration.setAllowCredentials(true);
        
        // How long the response to preflight requests can be cached
        configuration.setMaxAge(3600L);
        
        // Apply configuration to all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
