package com.librarysystem.userservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * A filter to validate requests coming from the API Gateway.
 * Ensures that only trusted requests with the correct gateway secret are processed.
 */
@Component
public class GatewayValidationFilter extends OncePerRequestFilter {

    @Value("${gateway.secret}")
    private String gatewaySecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
                // Retrieve the custom header from the request
        String gatewayHeader = request.getHeader("X-Gateway-Secret");

        // Validate the header value against the configured secret
        if (gatewayHeader == null || !gatewayHeader.equals(gatewaySecret)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthorized request");
            return;
        }

        // Proceed with the filter chain if validation passes
        filterChain.doFilter(request, response);
    }
}