package com.librarysystem.userservice.config;

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
public class SecurityConfig {

    private final GatewayValidationFilter gatewayValidationFilter;
    private final RoleExtractionFilter roleExtractionFilter;

    public SecurityConfig(GatewayValidationFilter gatewayValidationFilter, RoleExtractionFilter roleExtractionFilter) {
        this.gatewayValidationFilter = gatewayValidationFilter;
        this.roleExtractionFilter = roleExtractionFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        System.out.println("securityfilterchain working with GatewayValidationFilter and RoleExtractionFilter");
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Add GatewayValidationFilter first to ensure the request is from the gateway
            .addFilterBefore(gatewayValidationFilter, UsernamePasswordAuthenticationFilter.class)
            // Add RoleExtractionFilter after gateway validation but before authorization
            .addFilterAfter(roleExtractionFilter, GatewayValidationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Public endpoint for user creation (POST /api/users)
                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                // Endpoint for auth-service to validate credentials
                .requestMatchers(HttpMethod.POST, "/api/users/validate").permitAll()
                // Example: Secure GET /api/users/{id} - requires MEMBER, LIBRARIAN, or ADMIN
                .requestMatchers(HttpMethod.GET, "/api/users/{id:[\\d+]}").hasAnyRole("MEMBER", "LIBRARIAN", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/users/email/{email}").hasAnyRole("MEMBER", "LIBRARIAN", "ADMIN")
                // Example: Secure DELETE /api/users/{id} - requires ADMIN
                .requestMatchers(HttpMethod.DELETE, "/api/users/{id:[\\d+]}").hasRole("ADMIN")
                // Secure all other /api/users/** endpoints - requires LIBRARIAN or ADMIN by default
                .requestMatchers("/api/users/**").hasAnyRole("LIBRARIAN", "ADMIN")
                // Fallback: any other request not matched above should be denied by default if not explicitly permitted
                .anyRequest().authenticated() // Or .denyAll() if you want to be more restrictive
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}