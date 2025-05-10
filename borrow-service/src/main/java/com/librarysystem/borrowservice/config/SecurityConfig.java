package com.librarysystem.borrowservice.config;

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
 * Security configuration for the Book Service.
 * 
 * This class configures Spring Security for the Book Service microservice, defining
 * authorization rules, filter chains, and security-related behavior. It integrates
 * with the API Gateway's security model by applying two crucial filters:
 * 1. GatewayValidationFilter - Ensures requests originate from our trusted API Gateway
 * 2. RoleExtractionFilter - Extracts user roles from headers to enable authorization
 * 
 * The configuration implements a defense-in-depth approach to security within our
 * microservice architecture, with stateless authentication and fine-grained 
 * role-based access control.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // To enable @PreAuthorize, @PostAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayValidationFilter gatewayValidationFilter;
    private final RoleExtractionFilter roleExtractionFilter;

    /**
     * Defines the security filter chain and authorization rules.
     * 
     * This method:
     * 1. Configures stateless security appropriate for a microservice
     * 2. Sets up filters in the correct order of execution
     * 3. Defines endpoint access based on HTTP method and user role
     * 
     * The authorization rules implement a least-privilege model where:
     * - Reading books (GET) requires any authenticated role (MEMBER, LIBRARIAN, ADMIN)
     * - Modifying books (POST, PUT, DELETE) requires elevated privileges (LIBRARIAN, ADMIN)
     * 
     * @param http The HttpSecurity to configure
     * @return The configured SecurityFilterChain
     * @throws Exception If security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Disable CSRF as we are stateless and rely on secrets/tokens
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Ensure SecurityContext is not saved in HttpSession
            .securityContext(securityContext -> securityContext
                .securityContextRepository(new RequestAttributeSecurityContextRepository())
            )
            .addFilterBefore(gatewayValidationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(roleExtractionFilter, GatewayValidationFilter.class)
            .authorizeHttpRequests(authorize -> authorize
                // Allow all authenticated users to borrow books
                .requestMatchers(HttpMethod.POST, "/api/borrow/**").hasAnyRole("MEMBER", "LIBRARIAN", "ADMIN")
                // Allow users to view their own borrows
                .requestMatchers(HttpMethod.GET, "/api/borrow/**").hasAnyRole("MEMBER", "LIBRARIAN", "ADMIN")
                // (Optional) Only LIBRARIAN or ADMIN can delete or update borrows
                .requestMatchers(HttpMethod.DELETE, "/api/borrow/**").hasAnyRole("LIBRARIAN", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/borrow/**").hasAnyRole("LIBRARIAN", "ADMIN")
                // Fallback: any other request must be authenticated
                .anyRequest().authenticated()
            );

        return http.build();
    }
}
