package com.librarysystem.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Data Transfer Object for JWT token response.
 * Encapsulates the JWT token returned after successful authentication.
 *
 * Structure:
 * - token: JWT string containing:
 *   - Header (algorithm, token type)
 *   - Payload (user claims)
 *   - Signature
 *
 * Usage:
 * - Returned by /api/auth/login endpoint
 * - Token should be included in Authorization header for subsequent requests
 * - Format: Bearer <token>
 */
@Data               // Lombok: generates getters, setters, equals, hashCode, toString
@AllArgsConstructor // Lombok: generates constructor with all fields
public class JwtResponse {
    /**
     * The JWT token string.
     * Format: xxxxx.yyyyy.zzzzz
     * - xxxxx: Base64URL encoded header
     * - yyyyy: Base64URL encoded payload
     * - zzzzz: Cryptographic signature
     */
    private String token;
}