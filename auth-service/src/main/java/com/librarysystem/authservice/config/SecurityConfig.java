package com.librarysystem.authservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the auth service.
 * Sets up JWT authentication, CSRF protection, and password encryption.
 * 
 * Key features:
 * - Stateless session management (no session cookies)
 * - Public access to /api/auth/** endpoints
 * - BCrypt password encoding
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures security filter chain with custom settings.
     * 
     * Security rules:
     * 1. Disable CSRF (not needed for stateless API)
     * 2. Public access to auth endpoints (/api/auth/**)
     * 3. All other endpoints (if any) would require authentication (but typically auth-service only has public auth endpoints)
     * 4. No session tracking (stateless)
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()  // Login and other auth-related endpoints are public
                .anyRequest().authenticated()                 // Any other hypothetical endpoint would need auth (and rely on headers from Gateway)
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));  // No sessions
            
        return http.build();
    }

    /**
     * Creates password encoder for secure password handling.
     * This is used by user-service, but auth-service doesn't directly use it
     * if credential validation is fully delegated. However, it's harmless to keep.
     * If you were to implement password change/reset within auth-service, it might be needed.
     * @return PasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}