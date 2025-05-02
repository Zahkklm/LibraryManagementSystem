package com.librarysystem.authservice.controller;

import com.librarysystem.authservice.client.UserServiceClient;
import com.librarysystem.authservice.dto.JwtResponse;
import com.librarysystem.authservice.dto.LoginRequest;
import com.librarysystem.authservice.security.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller handling authentication endpoints.
 * Provides login functionality and JWT token generation.
 * Base path: /api/auth
 *
 * Security Notes:
 * - Uses stateless authentication with JWT
 * - Validates credentials against user-service
 * - Returns JWT token for successful authentication
 * - All endpoints are public (configured in SecurityConfig)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor  // Lombok: generates constructor for final fields
public class AuthController {

    /**
     * Feign client for user service communication.
     * Handles credential validation against user database.
     * Circuit breaker enabled for fault tolerance.
     */
    private final UserServiceClient userServiceClient;
    
    /**
     * JWT utilities for token operations.
     * Handles token generation, validation, and parsing.
     * Configured with secret key and expiration time.
     */
    private final JwtUtils jwtUtils;

    /**
     * Handles user login requests.
     * Validates credentials against user-service and generates JWT token.
     *
     * Authentication flow:
     * 1. Validate request body
     * 2. Check credentials with user-service
     * 3. Generate JWT token if valid
     * 4. Return token or 401 unauthorized
     *
     * @param request LoginRequest containing email and password
     * @return ResponseEntity with JWT token if authentication successful, 401 otherwise
     * @throws jakarta.validation.ValidationException if request validation fails
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            // Step 1: Validate credentials using user-service
            boolean isValid = userServiceClient.validateCredentials(request);

            if (isValid) {
                // Step 2: Create authentication object for token generation
                // Note: Password is null as it's already validated
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(request.getEmail(), null);
                
                // Step 3: Generate JWT token with user details
                String token = jwtUtils.generateToken(authentication);
                
                // Step 4: Return token in response body
                return ResponseEntity.ok(new JwtResponse(token));
            }

            // Step 5: Return 401 Unauthorized if credentials are invalid
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            
        } catch (Exception e) {
            // Handle any unexpected errors during authentication
            // Returns 401 to avoid exposing internal errors
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}