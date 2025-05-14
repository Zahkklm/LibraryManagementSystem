package com.librarysystem.authservice.service;

import com.librarysystem.authservice.client.KeycloakClient;
import com.librarysystem.authservice.client.UserServiceClient;
import com.librarysystem.authservice.dto.JwtResponse;
import com.librarysystem.authservice.dto.LoginRequest;
import com.librarysystem.authservice.dto.UserCreateRequest;
import com.librarysystem.authservice.dto.UserDTO;
import feign.FeignException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

/**
 * Service class for authentication-related business logic.
 * Handles login and registration by orchestrating calls to user-service and Keycloak.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    // Feign client for user-service communication
    private final UserServiceClient userServiceClient;
    // Feign client for Keycloak token requests
    private final KeycloakClient keycloakClient;

    // Keycloak client credentials (injected from application properties)
    @Value("${keycloak.client-id}")
    private String keycloakClientId;

    @Value("${keycloak.client-secret}")
    private String keycloakClientSecret;

    /**
     * Handles user login.
     * 1. Validates credentials with user-service.
     * 2. Requests JWT from Keycloak if credentials are valid.
     * 3. Returns JWT token or appropriate error response.
     *
     * @param request LoginRequest containing email and password.
     * @return ResponseEntity with JwtResponse or error status.
     */
    public ResponseEntity<JwtResponse> login(@Valid LoginRequest request) {
        try {
            // Step 1: Validate credentials with user-service
            boolean isValid = userServiceClient.validateCredentials(request);
            logger.debug("Credentials validation for {}: {}", request.getEmail(), isValid);

            if (!isValid) {
                logger.warn("Invalid credentials for user: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Step 2: Request a token from Keycloak using ROPC grant
            MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
            formParams.add("client_id", keycloakClientId);
            formParams.add("client_secret", keycloakClientSecret);
            formParams.add("grant_type", "password");
            formParams.add("username", request.getEmail());
            formParams.add("password", request.getPassword());
            formParams.add("scope", "openid profile email");

            logger.info("Requesting token from Keycloak for user: {}", request.getEmail());
            ResponseEntity<Map<String, Object>> keycloakResponse = keycloakClient.getToken(formParams);

            // Step 3: Handle Keycloak response
            if (keycloakResponse.getStatusCode() == HttpStatus.OK && keycloakResponse.getBody() != null) {
                String accessToken = (String) keycloakResponse.getBody().get("access_token");
                if (accessToken != null) {
                    logger.info("Successfully obtained token from Keycloak for user: {}", request.getEmail());
                    return ResponseEntity.ok(new JwtResponse(accessToken));
                } else {
                    logger.error("Keycloak response did not contain access_token for user: {}", request.getEmail());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            } else {
                logger.error("Failed to obtain token from Keycloak. Status: {}, Body: {}", keycloakResponse.getStatusCode(), keycloakResponse.getBody());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

        } catch (FeignException.FeignClientException e) {
            // Handle client errors from Feign (e.g., 4xx)
            logger.error("FeignClientException during Keycloak token request for user {}: Status {} - Response Body: {}", request.getEmail(), e.status(), e.contentUTF8(), e);
            return ResponseEntity.status(e.status()).build();
        } catch (FeignException e) {
            // Handle other Feign exceptions
            logger.error("FeignException during Keycloak token request for user {}: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            // Handle any other exceptions
            logger.error("Exception during login for user {}: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Handles user registration.
     * 1. Calls user-service to create the user (which also creates in Keycloak).
     * 2. Optionally logs in the user after registration to return a JWT.
     * 3. Returns created user info and JWT token, or error response.
     *
     * @param request UserCreateRequest containing registration details.
     * @return ResponseEntity with user info and JWT, or error status.
     */
    public ResponseEntity<?> register(@Valid UserCreateRequest request) {
        try {
            // 1. Call user-service to create the user (user-service will create in Keycloak and DB)
            UserDTO userDTO = userServiceClient.createUser(request);

            // 2. Optionally, auto-login after registration (get JWT from Keycloak)
            LoginRequest loginRequest = new LoginRequest(request.getEmail(), request.getPassword());
            JwtResponse jwtResponse = this.login(loginRequest).getBody();

            // 3. Return user info, Keycloak UUID and JWT
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                        "user", userDTO, 
                        "keycloakId", userDTO.getId(),  // Explicitly include the Keycloak UUID
                        "token", jwtResponse != null ? jwtResponse.getToken() : null
                    ));
        } catch (FeignException.Conflict e) {
            // Handle conflict if email already exists
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
        } catch (Exception e) {
            // Handle any other exceptions
            logger.error("Registration failed for email {}: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Registration failed");
        }
    }
}