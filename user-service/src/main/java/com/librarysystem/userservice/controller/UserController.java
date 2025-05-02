package com.librarysystem.userservice.controller;

import com.librarysystem.userservice.dto.LoginRequest;
import com.librarysystem.userservice.dto.UserCreateRequest;
import com.librarysystem.userservice.entity.User;
import com.librarysystem.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing user operations.
 * Provides endpoints for creating, retrieving, and deactivating users.
 * Base path: /api/users
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a new user in the system.
     *
     * @param request The user creation request containing user details
     * @return ResponseEntity containing the created User object
     * @throws jakarta.validation.ValidationException if request validation fails
     */
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.ok(userService.createUser(request));
    }

    /**
     * Retrieves a user by their unique identifier.
     *
     * @param id The unique identifier of the user
     * @return ResponseEntity containing the User object if found
     * @throws RuntimeException if user is not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /**
     * Retrieves a user by their email address.
     *
     * @param email The email address of the user
     * @return ResponseEntity containing the User object if found
     * @throws RuntimeException if user is not found
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<User> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    /**
     * Deactivates a user account.
     * This operation sets the user's active status to false but does not delete the record.
     *
     * @param id The unique identifier of the user to deactivate
     * @return ResponseEntity with no content on successful deactivation
     * @throws RuntimeException if user is not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Validates user credentials.
     * Used by auth-service for user authentication.
     * Checks both password match and account status.
     *
     * @param request DTO containing login credentials
     * @return ResponseEntity with boolean indicating validity
     */
    @PostMapping("/validate")
    public ResponseEntity<Boolean> validateCredentials(@Valid @RequestBody LoginRequest request) {
        try {
            User user = userService.getUserByEmail(request.getEmail());
            boolean isValid = passwordEncoder.matches(request.getPassword(), user.getPassword());
            return ResponseEntity.ok(isValid && user.isActive());
        } catch (RuntimeException e) {
            return ResponseEntity.ok(false);  // Return false for non-existent users
        }
    }
}