package com.librarysystem.authservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Base64;
import javax.crypto.SecretKey;

/**
 * Utility class for JWT operations.
 * Handles token generation, validation, and parsing.
 */
public class JwtUtils {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtils(String secret, long expirationMs) {
        // Ensure minimum key length for HS512 (512 bits = 64 bytes)
        byte[] keyBytes = new byte[64];
        
        if (secret == null || secret.isEmpty()) {
            // Generate secure random key if no secret provided
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(keyBytes);
        } else {
            // Use provided secret, but ensure it meets length requirements
            byte[] providedBytes = Base64.getDecoder().decode(secret);
            if (providedBytes.length < 64) {
                throw new IllegalArgumentException("Secret key must be at least 512 bits (64 bytes) for HS512");
            }
            System.arraycopy(providedBytes, 0, keyBytes, 0, Math.min(providedBytes.length, 64));
        }
        
        // Create secure key for HS512 algorithm
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * Generates JWT token for authenticated user.
     *
     * @param authentication the authentication object containing user details
     * @return JWT token string
     */
    public String generateToken(Authentication authentication) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
            .setSubject(authentication.getName())
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(key, SignatureAlgorithm.HS512)
            .compact();
    }

    /**
     * Validates JWT token.
     *
     * @param token the token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts username from JWT token.
     *
     * @param token the token to parse
     * @return username from token subject
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();

        return claims.getSubject();
    }
}