package com.librarysystem.userservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
 * A filter to extract roles from the X-User-Roles header (set by API Gateway)
 * and populate the Spring SecurityContext.
 */
@Component
public class RoleExtractionFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RoleExtractionFilter.class);
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String rolesHeader = request.getHeader(USER_ROLES_HEADER);

        if (rolesHeader != null && !rolesHeader.isEmpty()) {
            logger.debug("Found {} header: {}", USER_ROLES_HEADER, rolesHeader);
            List<GrantedAuthority> authorities = Arrays.stream(rolesHeader.split(","))
                    .map(String::trim)
                    .filter(role -> !role.isEmpty())
                    .map(role -> "ROLE_" + role.toUpperCase()) // Ensure "ROLE_" prefix for hasRole/hasAnyRole
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            if (!authorities.isEmpty()) {
                // Using a principal name like "gateway-user" as the actual user identity is managed by the gateway
                // and the token. This principal is mainly for Spring Security context.
                Authentication authentication = new UsernamePasswordAuthenticationToken("gateway-user", null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Set SecurityContext with authorities: {}", authorities);
            } else {
                logger.debug("No authorities extracted from {} header.", USER_ROLES_HEADER);
                SecurityContextHolder.getContext().setAuthentication(null); // Clear context if no valid roles
            }
        } else {
            logger.trace("No {} header found in the request.", USER_ROLES_HEADER);
            // If no roles header, clear context or handle as anonymous, depending on policy
            // For now, clearing it ensures that if this filter is active, roles MUST come from the header.
             SecurityContextHolder.getContext().setAuthentication(null);
        }

        filterChain.doFilter(request, response);
    }
}