package com.librarysystem.authservice.controller;

import com.librarysystem.authservice.dto.JwtResponse;
import com.librarysystem.authservice.dto.LoginRequest;
import com.librarysystem.authservice.dto.UserCreateRequest;
import com.librarysystem.authservice.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthController.
 * Verifies that controller methods delegate to AuthService.
 */
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void login_shouldDelegateToAuthService_andReturnJwtResponse() {
        // Arrange
        LoginRequest request = new LoginRequest("test@example.com", "password");
        JwtResponse jwtResponse = new JwtResponse("jwt-token");
        when(authService.login(request)).thenReturn(ResponseEntity.ok(jwtResponse));

        // Act
        ResponseEntity<JwtResponse> response = authController.login(request);

        // Assert
        assertEquals(jwtResponse, response.getBody());
        verify(authService, times(1)).login(request);
    }

    @Test
    void register_shouldDelegateToAuthService_andReturnResponse() {
        // Arrange
        UserCreateRequest createRequest = new UserCreateRequest();
        createRequest.setFirstName("Test");
        createRequest.setLastName("User");
        createRequest.setEmail("test@example.com");
        createRequest.setPassword("password");
        ResponseEntity<String> expectedResponse = ResponseEntity.ok("registered");
        when(authService.register(createRequest)).thenReturn((ResponseEntity) expectedResponse);
        
        // Act
        ResponseEntity<?> response = authController.register(createRequest);

        // Assert
        assertEquals(expectedResponse, response);
        verify(authService, times(1)).register(createRequest);
    }
}