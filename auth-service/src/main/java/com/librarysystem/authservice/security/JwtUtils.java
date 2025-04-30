package com.library.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Date;

/**
 * JWT Utility class for handling JWT token operations including:
 * - Token generation
 * - Token validation
 * - Claim extraction
 *
 * Uses io.jsonwebtoken library for JWT operations and Spring Value injection for configuration.
 */
@Component
public class JwtUtils {

  // Secret key for signing and verifying JWT tokens (injected from application properties)
  @Value("${jwt.secret}")
  private String secret;

  // Token expiration time in milliseconds (injected from application properties)
  @Value("${jwt.expiration}")
  private long expiration;

  /**
   * Generates a JWT token for a user with specified role.
   *
   * @param username Subject of the token (typically user identifier)
   * @param role User role to be included as a claim
   * @return Signed JWT token as compact string
   */
  public String generateToken(String username, String role) {
    return Jwts.builder()
      .subject(username)  // Set subject (typically username)
      .claim("role", role) // Add custom claim for user role
      .issuedAt(new Date())  // Set token issuance time
      .expiration(new Date(System.currentTimeMillis() + expiration)) // Set expiration time
      .signWith(SignatureAlgorithm.HS512, secret) // Sign with HS512 algorithm and secret key
      .compact();  // Convert to compact URL-safe string
  }

  /**
   * Extracts username from JWT token.
   *
   * @param token JWT token to parse
   * @return Username extracted from token subject
   * @throws io.jsonwebtoken.JwtException If token is invalid or signature verification fails
   */
  public String getUsernameFromToken(String token) {
    return Jwts.parser()
      .setSigningKey(secret)  // Set secret key for signature verification
      .build()
      .parseClaimsJws(token)  // Parse and verify token signature
      .getBody()
      .getSubject();  // Extract subject (username) from claims
  }

  /**
   * Validates JWT token integrity and expiration.
   *
   * @param token JWT token to validate
   * @return true if token is valid and not expired, false otherwise
   */
  public boolean validateToken(String token) {
    try {
      Jwts.parser()
        .setSigningKey(secret)  // Verify using secret key
        .build()
        .parseClaimsJws(token);  // Throws exceptions for invalid/expired tokens
      return true;
    } catch (Exception e) {
      // Catch all JWT-related exceptions (ExpiredJwtException, SignatureException, etc)
      return false;
    }
  }
}

// Security Considerations:
// 1. The secret key should be protected and not hardcoded
// 2. Consider using stronger algorithm like HS512 (as shown)
// 3. Ensure secure transmission of tokens (HTTPS only)

// Potential Improvements:
// 1. Add specific exception handling for different error types
// 2. Implement token revocation mechanism
// 3. Add logging for token generation/validation events
// 4. Create method to extract roles from claims
// 5. Add token refresh capability
// 6. Implement token blacklisting
// 7. Add input validation for empty/malformed tokens
// 8. Consider using dedicated SecurityProvider for key management
