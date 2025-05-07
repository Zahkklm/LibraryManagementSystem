package com.librarysystem.userservice.controller;

import com.librarysystem.userservice.dto.LoginRequest;
import com.librarysystem.userservice.dto.UserCreateRequest;
import com.librarysystem.userservice.dto.UserDTO; // Ensure this imports your user-service DTO
import com.librarysystem.userservice.entity.User;
import com.librarysystem.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    // Helper method to check roles from header
    private boolean hasRole(String rolesHeader, String expectedRole) {
        if (rolesHeader == null || rolesHeader.isEmpty()) {
            logger.trace("Role check for '{}': Header is null or empty.", expectedRole);
            return false;
        }
        boolean found = Arrays.asList(rolesHeader.split(",")).contains(expectedRole);
        logger.trace("Role check for '{}' in header '{}': {}", expectedRole, rolesHeader, found);
        return found;
    }

    private boolean hasAnyRole(String rolesHeader, String... expectedRoles) {
        if (rolesHeader == null || rolesHeader.isEmpty()) {
            logger.trace("Role check for any of '{}': Header is null or empty.", Arrays.toString(expectedRoles));
            return false;
        }
        List<String> actualRoles = Arrays.asList(rolesHeader.split(","));
        for (String expectedRole : expectedRoles) {
            if (actualRoles.contains(expectedRole)) {
                logger.trace("Role check for any of '{}': Found '{}' in header '{}'.", Arrays.toString(expectedRoles), expectedRole, rolesHeader);
                return true;
            }
        }
        logger.trace("Role check for any of '{}': No match found in header '{}'.", Arrays.toString(expectedRoles), rolesHeader);
        return false;
    }

    @PostMapping
    public ResponseEntity<UserDTO> createUser(
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader,
            @Valid @RequestBody UserCreateRequest request) {
        logger.info("Attempting to create user: {}", request.getEmail());
        // Example: Only ADMIN or LIBRARIAN can create users.
        // This check might be redundant if API Gateway already enforces it for the /api/users POST path.
        // However, it's good for defense in depth.
        // If rolesHeader is null/empty, it means the call might be from a service or unauthenticated context not meant for user creation by end-users.
        // Or, if API Gateway is the sole enforcer for this path, this check could be more lenient or removed.
        if (rolesHeader == null || !hasAnyRole(rolesHeader, "ADMIN", "LIBRARIAN")) {
            // Allow creation if no roles header is present, assuming it's a trusted internal call or public registration if intended.
            // For stricter control, you might require the header and specific roles.
            // For now, let's assume if the header is present, it must contain ADMIN or LIBRARIAN.
            // If the header is NOT present, we might allow it if the API gateway has already authorized the request path.
            // This logic needs to be aligned with your API Gateway's security policy for this endpoint.
            // If this endpoint is *only* for ADMIN/LIBRARIAN creation, then `required=true` on header and stricter check.
            logger.warn("User creation attempt by {} with roles: {}. API Gateway should primarily enforce this.", request.getEmail(), rolesHeader);
            // For this example, let's proceed if rolesHeader is null (e.g. direct call for testing, or gateway allows)
            // but if rolesHeader IS present, it must be ADMIN or LIBRARIAN.
            if (rolesHeader != null && !hasAnyRole(rolesHeader, "ADMIN", "LIBRARIAN")) {
                 logger.warn("Insufficient permissions to create user {} with roles {}. Required ADMIN or LIBRARIAN.", request.getEmail(), rolesHeader);
                 throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions to create user. Header present but roles insufficient.");
            }
        }
        User user = userService.createUser(request);
        logger.info("User created successfully: {}", user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDTO.fromEntity(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateUser(
            @RequestHeader(name = "X-User-Roles", required = true) String rolesHeader,
            @PathVariable Long id) {
        logger.info("Attempting to deactivate user with ID: {} by user with roles: {}", id, rolesHeader);
        if (!hasRole(rolesHeader, "ADMIN")) {
            logger.warn("Deactivation failed for user ID {}: Insufficient permissions. Roles: {}", id, rolesHeader);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only ADMIN can deactivate users");
        }
        userService.deactivateUser(id);
        logger.info("User with ID: {} deactivated successfully.", id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(
            @RequestHeader(name = "X-User-Roles", required = true) String rolesHeader,
            @PathVariable Long id) {
        logger.debug("Attempting to get user by ID: {} with roles: {}", id, rolesHeader);
        if (!hasAnyRole(rolesHeader, "ADMIN", "LIBRARIAN")) {
            logger.warn("Access denied for user ID {}: Insufficient permissions. Roles: {}", id, rolesHeader);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
        User user = userService.getUserById(id);
        logger.debug("User found by ID {}: {}", id, user.getEmail());
        return ResponseEntity.ok(UserDTO.fromEntity(user));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UserDTO> getUserByEmail(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(name = "X-User-Roles", required = false) String rolesHeader,
            @PathVariable String email) {
        logger.debug("Attempting to get user by email: {} (Caller roles: {})", email, rolesHeader);
        // This endpoint is used by auth-service (won't have X-User-Roles from an end-user context)
        // and potentially by end-users via API Gateway (will have X-User-Roles).
        // For now, let's assume it's generally accessible if API Gateway allows the path,
        // as auth-service needs it for its operations.
        // If an end-user calls this, the API Gateway should have already authenticated them.
        // Specific role checks could be added if needed for end-user access.
        User user = userService.getUserByEmail(email);
        logger.debug("User found by email {}: {}", email, user.getEmail());
        return ResponseEntity.ok(UserDTO.fromEntity(user));
    }

    @PostMapping("/validate")
    public ResponseEntity<Boolean> validateCredentials(@Valid @RequestBody LoginRequest request) {
        logger.debug("Validating credentials for email: {}", request.getEmail());
        // This endpoint is called by auth-service and doesn't need user context headers.
        try {
            User user = userService.getUserByEmail(request.getEmail());
            boolean isValid = passwordEncoder.matches(request.getPassword(), user.getPassword());
            logger.debug("Credential validation for {}: active={}, passwordMatch={}", request.getEmail(), user.isActive(), isValid);
            return ResponseEntity.ok(isValid && user.isActive());
        } catch (RuntimeException e) {
            logger.warn("Credential validation failed for email {}: User not found or other error - {}", request.getEmail(), e.getMessage());
            return ResponseEntity.ok(false); // User not found or other issue, so credentials are not valid.
        }
    }
}