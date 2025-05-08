package com.librarysystem.authservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(GatewayValidationFilter.class);

    @Value("${gateway.secret}")
    private String gatewaySecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        logger.debug("GatewayValidationFilter processing request for: {}", requestPath);

        // Retrieve the custom header from the request
        String gatewayHeader = request.getHeader("X-Gateway-Secret");

        // Validate the header value against the configured secret
        if (gatewayHeader == null || !gatewayHeader.equals(gatewaySecret)) {
            logger.warn("Unauthorized request to {}: Missing or incorrect X-Gateway-Secret header. IP: {}", requestPath, request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthorized request: Invalid or missing gateway secret.");
            return;
        }
        
        logger.debug("X-Gateway-Secret validated successfully for request: {}", requestPath);
        // Proceed with the filter chain if validation passes
        filterChain.doFilter(request, response);
    }
}