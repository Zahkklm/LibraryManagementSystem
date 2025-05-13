package com.librarysystem.authservice.controller;

import com.librarysystem.authservice.dto.JwtResponse;
import com.librarysystem.authservice.dto.LoginRequest;
import com.librarysystem.authservice.dto.UserCreateRequest;
import com.librarysystem.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 * Delegates business logic to AuthService.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    // Injected AuthService handles authentication logic
    private final AuthService authService;

    /**
     * Handles user login.
     * @param request LoginRequest containing email and password.
     * @return JWT token if authentication is successful.
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * Handles user registration.
     * @param request UserCreateRequest containing registration details.
     * @return Created user info and JWT token if registration is successful.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserCreateRequest request) {
        return authService.register(request);
    }
}