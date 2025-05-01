package com.librarysystem.authservice.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

/**
 * JWT Utilities handling token generation, validation, and claims extraction.
 * <p>
 * Implements RFC 7519 standards for JSON Web Tokens using the jjwt library.
 * Utilizes HMAC-SHA512 for token signing by default.
 *
 * Security Considerations:
 * - Secret key should be at least 512 bits (64 bytes) for HS512
 * - Token expiration should be set based on security requirements
 * - Sensitive claims should be avoided in JWT payload
 * - Token validation should be the first step in any JWT processing
 */
@Component
public class JwtUtils {
  private static final String ISSUER = "LibraryAuthService";
  private final Key jwtSecret;
  private final long jwtExpirationMs;

  /**
   * Initializes JWT processor with crypto-safe key derivation
   * @param secret Base64 encoded 512-bit (64 byte) secret
   * @param expirationMs Token TTL in milliseconds (recommended ≤ 1 hour)
   */
  public JwtUtils(
    @Value("${app.jwt.secret}") String secret,
    @Value("${app.jwt.expiration-ms}") long expirationMs
  ) {
    this.jwtSecret = Keys.hmacShaKeyFor(secret.getBytes()); // Key derivation function
    this.jwtExpirationMs = expirationMs;
  }

  /**
   * Resolves username from validated token claims
   * @throws JwtException if claims extraction fails validation
   */
  public String getUsernameFromToken(String token) throws JwtException {
    return extractUsername(token)
      .orElseThrow(() -> new JwtException("Invalid subject claim"));
  }

  /**
   * Generates a signed JWT for authenticated users
   *
   * @param authentication Spring Security authentication object
   * @return Compact URL-safe JWT string
   *
   * Token Structure:
   * - Subject: Authenticated username
   * - Issuer: LibraryAuthService
   * - Issued At: Current timestamp
   * - Expiration: Configurable time offset
   * - Signature: HMAC-SHA512 of header+payload
   */
  public String generateToken(Authentication authentication) {
    return Jwts.builder()
      .setSubject(authentication.getName())  // Unique user identifier
      .setIssuer(ISSUER)                     // Token origin verification
      .setIssuedAt(Date.from(Instant.now())) // Token creation timestamp
      .setExpiration(Date.from(Instant.now() // Calculated expiration
        .plus(jwtExpirationMs, ChronoUnit.MILLIS)))
      .signWith(jwtSecret, SignatureAlgorithm.HS512) // Cryptographic signature
      .compact(); // Finalize and serialize
  }

  /**
   * Extracts username from JWT subject claim
   *
   * @param token Compact JWT string
   * @return Optional containing username if valid, empty otherwise
   */
  public Optional<String> extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  /**
   * Validates JWT integrity and expiration
   *
   * @param token Compact JWT string
   * @return true if token is properly signed and not expired
   */
  public boolean validateToken(String token) {
    try {
      parseToken(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      // Security: Avoid logging raw tokens in production
      return false;
    }
  }

  /**
   * Generic claim extractor supporting custom claim resolution
   *
   * @param token Compact JWT string
   * @param claimsResolver Function to extract specific claim
   * @return Optional containing claim value if present and valid
   */
  private <T> Optional<T> extractClaim(String token, Function<Claims, T> claimsResolver) {
    return parseToken(token)
      .map(Jws::getBody)        // Extract verified claims body
      .map(claimsResolver);     // Apply custom claim resolver
  }

  /**
   * Parses and validates JWT structure
   *
   * @param token Compact JWT string
   * @return JWS claims if valid, empty for invalid/expired tokens
   *
   * Handled Exceptions:
   * - ExpiredJwtException: Token past expiration
   * - UnsupportedJwtException: Non-JWS/JWE token
   * - MalformedJwtException: Invalid JWT structure
   * - SignatureException: Signature verification failure
   * - IllegalArgumentException: Empty/null token
   */
  private Optional<Jws<Claims>> parseToken(String token) {
    try {
      return Optional.of(Jwts.parserBuilder()
        .setSigningKey(jwtSecret)  // Verify with configured secret
        .build()
        .parseClaimsJws(token));   // Validate signature + expiration
    } catch (ExpiredJwtException | UnsupportedJwtException |
             MalformedJwtException | SignatureException |
             IllegalArgumentException e) {
      // Security: Consider monitoring exception types for alerts
      return Optional.empty();
    }
  }
}
