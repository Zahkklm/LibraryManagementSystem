package com.library.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * JWT Authentication Filter that intercepts incoming requests to process JWT tokens.
 * This filter is executed once per request and is responsible for:
 * - Extracting JWT tokens from the Authorization header
 * - Validating JWT tokens
 * - Setting up Spring Security authentication context for valid tokens
 *
 * Inherits from OncePerRequestFilter to ensure single execution per request dispatch.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

  // Utility class for JWT operations (token validation, extraction of claims)
  private final JwtUtils jwtUtils;

  /**
   * Core method for filtering requests and handling JWT authentication.
   *
   * @param request  Incoming HTTP request
   * @param response HTTP response
   * @param filterChain Chain of filters to proceed with
   *
   * @throws ServletException If a servlet-related error occurs
   * @throws IOException If an I/O error occurs during request processing
   */
  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain)
    throws ServletException, IOException {

    // Step 1: Check for Authorization header with Bearer token
    final String header = request.getHeader("Authorization");

    // If no Authorization header or doesn't start with Bearer, continue filter chain without authentication
    if (header == null || !header.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;  // Exit filter processing early
    }

    // Step 2: Extract JWT token from header (remove "Bearer " prefix)
    final String token = header.substring(7);  // "Bearer ".length() = 7

    // Step 3: Validate token using JWT utility class
    if (jwtUtils.validateToken(token)) {

      // Extract username from valid JWT token
      final String username = jwtUtils.getUsernameFromToken(token);

      // Step 4: Create Authentication object and set it in SecurityContext
      // - Uses UsernamePasswordAuthenticationToken as simple authentication carrier
      // - Null credentials (password not needed after JWT validation)
      // - Null authorities (could be populated from token claims if needed)
      SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
          username,          // Principal (typically username)
          null,              // Credentials (not required here)
          null               // Authorities (could be extracted from token)
        )
      );
    }

    // Step 5: Continue processing the request through the filter chain
    // - If token was invalid, SecurityContext remains empty and subsequent security checks will fail
    filterChain.doFilter(request, response);
  }
}

// Potential Improvements:
// 1. Add error handling for invalid/malformed tokens
// 2. Populate authorities from token claims if using role-based authorization
// 3. Add logging for debugging purposes
// 4. Set response status codes for authentication failures
// 5. Handle token expiration explicitly if not handled in JwtUtils
// 6. Consider using SecurityContextHolder.clearContext() for invalid tokens
