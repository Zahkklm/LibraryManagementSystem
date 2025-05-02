package com.librarysystem.authservice.config;

import com.librarysystem.authservice.security.JwtAuthFilter;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the auth service.
 * Sets up JWT authentication, CSRF protection, and password encryption.
 * 
 * Key features:
 * - Stateless session management (no session cookies)
 * - JWT-based authentication
 * - Public access to /api/auth/** endpoints
 * - BCrypt password encoding
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // JWT filter that checks tokens in request headers
    private final JwtAuthFilter jwtAuthFilter;

    /**
     * Configures security filter chain with custom settings.
     * 
     * Security rules:
     * 1. Disable CSRF (not needed for stateless API)
     * 2. Public access to auth endpoints
     * 3. All other endpoints require authentication
     * 4. No session tracking (stateless)
     * 5. JWT filter runs before username/password authentication
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()  // Login endpoints are public
                .anyRequest().authenticated()                 // Everything else needs auth
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // No sessions
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Creates password encoder for secure password handling.
     * Uses BCrypt hashing function with default strength (10 rounds).
     * 
     * @return BCryptPasswordEncoder instance for password encoding
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}