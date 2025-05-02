package com.librarysystem.authservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JwtUtils.
 * Tests token generation and validation.
 */
class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private final String SECRET = "testSecretKeyWithLength512BitsForHS512SignatureAlgorithm";
    private final long EXPIRATION = 3600000; // 1 hour

    /**
     * Initializes JwtUtils with test configuration.
     */
    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(SECRET, EXPIRATION);
    }

    /**
     * Tests JWT token generation.
     * Verifies token is generated with correct structure.
     */
    @Test
    void whenGenerateToken_thenSuccess() {
        // Arrange
        Authentication auth = new UsernamePasswordAuthenticationToken(
            "test@example.com", null);

        // Act
        String token = jwtUtils.generateToken(auth);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // Header.Payload.Signature
    }

    /**
     * Tests JWT token validation.
     * Verifies valid token is properly validated.
     */
    @Test
    void whenValidToken_thenValidationSuccess() {
        // Arrange
        Authentication auth = new UsernamePasswordAuthenticationToken(
            "test@example.com", null);
        String token = jwtUtils.generateToken(auth);

        // Act
        boolean isValid = jwtUtils.validateToken(token);

        // Assert
        assertThat(isValid).isTrue();
    }

    /**
     * Tests username extraction from token.
     * Verifies correct username is extracted from valid token.
     */
    @Test
    void whenValidToken_thenExtractUsername() {
        // Arrange
        String username = "test@example.com";
        Authentication auth = new UsernamePasswordAuthenticationToken(
            username, null);
        String token = jwtUtils.generateToken(auth);

        // Act
        String extractedUsername = jwtUtils.getUsernameFromToken(token);

        // Assert
        assertThat(extractedUsername).isEqualTo(username);
    }
}