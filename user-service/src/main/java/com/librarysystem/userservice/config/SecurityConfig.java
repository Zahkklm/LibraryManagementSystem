package com.librarysystem.userservice.config;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Enable method-level security like @PreAuthorize
@RequiredArgsConstructor
public class SecurityConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    
    private final GatewayValidationFilter gatewayValidationFilter;
    private final RoleExtractionFilter roleExtractionFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring security filter chain with GatewayValidationFilter and RoleExtractionFilter");
        
        http.csrf(csrf -> {
                csrf.disable();
                logger.debug("CSRF protection disabled for API endpoints");
            })
            .sessionManagement(session -> {
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                logger.debug("Session management set to STATELESS");
            })
            // Add GatewayValidationFilter first to ensure the request is from the gateway
            .addFilterBefore(gatewayValidationFilter, UsernamePasswordAuthenticationFilter.class)
            // Add RoleExtractionFilter after gateway validation but before authorization
            .addFilterAfter(roleExtractionFilter, GatewayValidationFilter.class)
            .authorizeHttpRequests(auth -> {
                logger.debug("Configuring authorization rules for HTTP requests");
                // Allow Swagger UI and API docs without authentication
                auth
                .requestMatchers("/api/users/swagger-ui/**", "/api/users/swagger-ui.html", "/api/users/v3/api-docs/**"
                ,"/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll();
                auth
                    // Public endpoint for user creation (POST /api/users)
                    .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/users/validate").permitAll();
                logger.info("Public endpoints configured: POST /api/users, POST /api/users/validate");

                // Secure endpoints with role-based access
                auth
                    .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole("MEMBER", "LIBRARIAN", "ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/users/email/**").hasAnyRole("MEMBER", "LIBRARIAN", "ADMIN");
                logger.info("User profile endpoints configured with MEMBER+ access");
                
                auth
                    .requestMatchers(HttpMethod.DELETE, "/api/users/{id:.+}").hasRole("ADMIN");                logger.info("Admin endpoints configured: DELETE /api/users/{id}");
                
                // Default rules
                auth
                    .requestMatchers("/api/users/**").hasAnyRole("LIBRARIAN", "ADMIN")
                    .anyRequest().authenticated();
                logger.info("Default security rules applied");
            });

        logger.info("Security filter chain configuration completed");
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        logger.debug("Creating BCryptPasswordEncoder bean");
        return new BCryptPasswordEncoder();
    }
}
