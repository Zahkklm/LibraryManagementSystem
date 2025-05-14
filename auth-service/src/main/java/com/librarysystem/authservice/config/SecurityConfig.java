package com.librarysystem.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // Required for addFilterBefore

/**
 * Security configuration for the auth-service.
 *
 * Key features:
 * - Enables Spring Security's web security support.
 * - Optionally enables method-level security (e.g., @PreAuthorize).
 * - Disables CSRF protection (common for stateless APIs).
 * - Configures stateless session management.
 * - Validates X-Gateway-Secret header for all requests.
 * - Defines authorization rules:
 *   - Allows public access to all /api/auth/** endpoints (after gateway secret validation).
 *   - Requires authentication for any other endpoints (if they were to be added).
 * - Provides a PasswordEncoder bean.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final GatewayValidationFilter gatewayValidationFilter;

    public SecurityConfig(GatewayValidationFilter gatewayValidationFilter) {
        this.gatewayValidationFilter = gatewayValidationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Add GatewayValidationFilter to run before other Spring Security filters
            // This ensures the X-Gateway-Secret is checked for all incoming requests.
            .addFilterBefore(gatewayValidationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(
                    "/api/auth/swagger-ui/**", "/api/auth/swagger-ui.html", "/api/auth/v3/api-docs/**",
                    "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
