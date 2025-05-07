package com.librarysystem.authservice.controller;

import com.librarysystem.authservice.client.KeycloakClient;
import com.librarysystem.authservice.client.UserServiceClient;
import com.librarysystem.authservice.dto.JwtResponse;
import com.librarysystem.authservice.dto.LoginRequest;

import feign.FeignException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserServiceClient userServiceClient;
    private final KeycloakClient keycloakClient;

    @Value("${keycloak.client-id}")
    private String keycloakClientId;

    @Value("${keycloak.client-secret}")
    private String keycloakClientSecret;

    /**
     * Handles login requests by validating credentials with user-service and obtaining a token from Keycloak.
     *
     * @param request The login request containing email and password.
     * @return A JWT response containing the access token if successful.
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
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
            logger.error("FeignClientException during Keycloak token request for user {}: Status {} - Response Body: {}", request.getEmail(), e.status(), e.contentUTF8(), e);
            return ResponseEntity.status(e.status()).build();
        } catch (FeignException e) {
            logger.error("FeignException during Keycloak token request for user {}: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            logger.error("Exception during login for user {}: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}