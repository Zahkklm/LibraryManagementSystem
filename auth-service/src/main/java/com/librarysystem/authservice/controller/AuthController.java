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
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    // Feign client for user service communication
    private final UserServiceClient userServiceClient;
    
    // JWT utilities for token operations
    private final JwtUtils jwtUtils;

    /**
     * Handles user login requests.
     * Validates credentials against user-service and generates JWT token.
     *
     * @param request LoginRequest containing email and password
     * @return JWT token if authentication successful
     * @throws jakarta.validation.ValidationException if request validation fails
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        // Validate credentials using user-service
        boolean isValid = userServiceClient.validateCredentials(request);

        if (isValid) {
            // Create authentication object with validated user details
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(request.getEmail(), null);
            
            // Generate JWT token for authenticated user
            String token = jwtUtils.generateToken(authentication);
            return ResponseEntity.ok(new JwtResponse(token));
        }

        // Return 401 Unauthorized if credentials are invalid
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}