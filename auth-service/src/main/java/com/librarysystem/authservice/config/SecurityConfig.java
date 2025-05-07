package com.librarysystem.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the auth service.
 * 
 * Key features:
 * - Public access to /api/auth/** endpoints
 * - Stateless session management
 * - CSRF protection disabled
 */
@Configuration
public class SecurityConfig {

    /**
     * Configures security filter chain with custom settings.
     * 
     * Security rules:
     * 1. Disable CSRF (not needed for stateless API)
     * 2. Public access to auth endpoints (/api/auth/**)
     * 3. Stateless session management
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable) // Disable CSRF for stateless APIs
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll() // Public access to auth endpoints
                .anyRequest().authenticated()               // Secure any other endpoints (if added in the future)
            )
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)); // Stateless session management

        return http.build();
    }

    /**
     * Creates a password encoder for secure password handling.
     * 
     * @return PasswordEncoder instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}