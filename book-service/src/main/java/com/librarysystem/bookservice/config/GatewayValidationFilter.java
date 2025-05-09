package com.librarysystem.bookservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security filter that validates if requests originate from the trusted API Gateway.
 * 
 * This filter enforces that all requests must include a valid gateway secret header,
 * ensuring that clients cannot bypass the API Gateway to directly access the Book Service.
 * The filter is part of the system's defense-in-depth approach to microservice security.
 */
@Component
public class GatewayValidationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(GatewayValidationFilter.class);
    
    /**
     * The gateway secret value configured in application.yml and injected here.
     * This must match the secret added by the API Gateway as a request header.
     */
    @Value("${api.gateway.secret}")
    private String gatewaySecret;

    /**
     * Header name used by the API Gateway to pass the secret to downstream services.
     */
    private static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";

    /**
     * Intercepts each request to validate the gateway secret header.
     * 
     * @param request The HTTP request received by the filter
     * @param response The HTTP response that will be sent back
     * @param filterChain The chain of filters that the request will pass through
     * @throws ServletException If an error occurs in request processing
     * @throws IOException If an I/O error occurs while handling the request
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String secretHeaderValue = request.getHeader(GATEWAY_SECRET_HEADER);

        if (gatewaySecret.equals(secretHeaderValue)) {
            // Secret is valid - continue to next filter in the chain
            filterChain.doFilter(request, response);
        } else {
            // Secret is invalid or missing - reject the request
            logger.warn("Invalid or missing gateway secret from IP: {}", request.getRemoteAddr());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("Unauthorized: Invalid Gateway Secret");
        }
    }
}