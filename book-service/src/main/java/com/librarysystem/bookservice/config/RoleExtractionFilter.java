package com.librarysystem.bookservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter that extracts user role information from HTTP headers and establishes the Spring Security context.
 * 
 * In the microservice architecture, the API Gateway validates JWTs and extracts user information,
 * then adds this information as headers (X-User-Id, X-User-Roles) to requests before forwarding 
 * them to downstream services. This filter processes those headers to set up the security context
 * so that methods with @PreAuthorize annotations can properly enforce role-based access control.
 * 
 * This filter is part of the defense-in-depth approach to security, working alongside
 * GatewayValidationFilter to ensure that only properly authenticated and authorized requests
 * from the API Gateway are processed.
 */
@Component
public class RoleExtractionFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RoleExtractionFilter.class);
    
    /**
     * Header containing comma-separated user roles from the API Gateway.
     * The API Gateway extracts these roles from the validated JWT token.
     */
    private static final String USER_ROLES_HEADER = "X-User-Roles";
    
    /**
     * Header containing the user identifier (UUID or similar) from the API Gateway.
     * This will be used as the principal in the security context.
     */
    private static final String USER_ID_HEADER = "X-User-Id"; // Or X-User-Email if you prefer

    /**
     * Processes each incoming HTTP request to extract user roles and establish security context.
     * 
     * This method:
     * 1. Extracts user ID and roles from HTTP headers
     * 2. Converts role strings into Spring Security authorities
     * 3. Creates an authentication token and sets it in the security context
     * 4. Handles error cases gracefully with appropriate logging
     * 
     * @param request The incoming HTTP request
     * @param response The HTTP response
     * @param filterChain The filter processing chain
     * @throws ServletException If servlet processing fails
     * @throws IOException If I/O operation fails
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String rolesHeader = request.getHeader(USER_ROLES_HEADER);
        String userId = request.getHeader(USER_ID_HEADER); // Principal can be user ID or email

        if (userId != null && !userId.isEmpty() && rolesHeader != null && !rolesHeader.isEmpty()) {
            try {
                // Convert comma-separated roles into Spring Security authorities
                // Adding the ROLE_ prefix is required for Spring's hasRole() method to work correctly
                List<GrantedAuthority> authorities = Arrays.stream(rolesHeader.split(","))
                        .map(String::trim)
                        .filter(role -> !role.isEmpty())
                        .map(role -> "ROLE_" + role) // Spring Security expects "ROLE_" prefix
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                if (!authorities.isEmpty()) {
                    // Create authentication token with user ID as principal and extracted authorities
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.debug("User {} authenticated with roles: {}", userId, authorities);
                } else {
                    logger.debug("No roles found in header for user {}", userId);
                    // Set up with no specific roles if header is present but empty after processing
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                // Log and clear security context on error to prevent partial authentication
                logger.error("Error processing roles header: {}", rolesHeader, e);
                SecurityContextHolder.clearContext(); // Clear context on error
            }
        } else {
            // No user credentials found, continue unauthenticated
            // Spring Security's configuration will determine if the request is rejected
            logger.trace("No user ID or roles header found in request to {}", request.getRequestURI());
        }

        // Always continue the filter chain - authorization decisions happen later
        filterChain.doFilter(request, response);
    }
}