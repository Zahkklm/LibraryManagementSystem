package com.librarysystem.borrowservice.config;

import com.librarysystem.borrowservice.config.GatewayValidationFilter;
import com.librarysystem.borrowservice.config.RoleExtractionFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

/**
 * Security configuration for the Borrow Service.
 *
 * Configures Spring Security for the Borrow Service microservice, defining
 * authorization rules, filter chains, and security-related behavior. Integrates
 * with the API Gateway's security model by applying:
 * 1. GatewayValidationFilter - Ensures requests originate from our trusted API Gateway
 * 2. RoleExtractionFilter - Extracts user roles from headers to enable authorization
 *
 * The configuration implements stateless authentication and fine-grained
 * role-based access control for borrow operations.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayValidationFilter gatewayValidationFilter;
    private final RoleExtractionFilter roleExtractionFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .securityContext(securityContext -> securityContext
                .securityContextRepository(new RequestAttributeSecurityContextRepository())
            )
            .addFilterBefore(gatewayValidationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(roleExtractionFilter, GatewayValidationFilter.class)
            .authorizeHttpRequests(authorize -> authorize
                // Allow Swagger UI and API docs without authentication
                .requestMatchers("/api/borrows/swagger-ui/**", "/api/borrows/swagger-ui.html", "/api/borrows/v3/api-docs/**"
                        ,"/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                // Allow all authenticated users (MEMBER, LIBRARIAN, ADMIN) to view their own borrows and borrowing history
                .requestMatchers(HttpMethod.GET, "/api/borrows/**").hasAnyRole("MEMBER", "LIBRARIAN", "ADMIN")
                // Allow users to create borrow requests and return books
                .requestMatchers(HttpMethod.POST, "/api/borrows").hasAnyRole("MEMBER", "LIBRARIAN", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/borrows/*/return").hasAnyRole("MEMBER", "LIBRARIAN", "ADMIN")
                // Allow users to cancel their own borrows
                .requestMatchers(HttpMethod.POST, "/api/borrows/*/cancel").hasAnyRole("MEMBER", "LIBRARIAN", "ADMIN")
                // Allow librarians/admins to view all borrowing history and overdue reports
                .requestMatchers(HttpMethod.GET, "/api/borrows/history/**").hasAnyRole("LIBRARIAN", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/borrows/overdue").hasAnyRole("LIBRARIAN", "ADMIN")
                // Fallback: any other request must be authenticated
                .anyRequest().authenticated()
            );

        return http.build();
    }
}