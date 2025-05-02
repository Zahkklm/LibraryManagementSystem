package com.librarysystem.authservice.controller;

import com.librarysystem.authservice.client.UserServiceClient;
import com.librarysystem.authservice.dto.LoginRequest;
import com.librarysystem.authservice.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AuthController.
 * Uses Mockito for mocking dependencies.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthController authController;

    private LoginRequest validRequest;

    /**
     * Sets up test fixtures before each test.
     * Initializes test data and configures mock behavior.
     */
    @BeforeEach
    void setUp() {
        validRequest = new LoginRequest();
        validRequest.setEmail("test@example.com");
        validRequest.setPassword("password123");
    }

    /**
     * Tests successful authentication flow.
     * Verifies controller returns JWT token for valid credentials.
     */
    @Test
    void whenValidCredentials_thenReturnsToken() {
        // Arrange
        when(userServiceClient.validateCredentials(any())).thenReturn(true);
        when(jwtUtils.generateToken(any())).thenReturn("test.jwt.token");

        // Act
        var response = authController.login(validRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getToken()).isEqualTo("test.jwt.token");
    }

    /**
     * Tests authentication failure flow.
     * Verifies controller returns 401 for invalid credentials.
     */
    @Test
    void whenInvalidCredentials_thenReturns401() {
        // Arrange
        when(userServiceClient.validateCredentials(any())).thenReturn(false);

        // Act
        var response = authController.login(validRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Tests service unavailability scenario.
     * Verifies proper error handling when user service is down.
     */
    @Test
    void whenUserServiceUnavailable_thenReturns401() {
        // Arrange
        when(userServiceClient.validateCredentials(any()))
            .thenThrow(new RuntimeException("Service unavailable"));

        // Act
        var response = authController.login(validRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}