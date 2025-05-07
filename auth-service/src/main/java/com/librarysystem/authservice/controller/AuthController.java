package com.librarysystem.authservice.controller;

import com.librarysystem.authservice.client.KeycloakClient; // Added
import com.librarysystem.authservice.client.UserServiceClient;
import com.librarysystem.authservice.dto.JwtResponse;
import com.librarysystem.authservice.dto.LoginRequest;
// import com.librarysystem.authservice.dto.UserDTO; // Keep this if still used elsewhere or remove

import feign.FeignException; // Added
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
// import org.springframework.stereotype.Component; // Removed if RestTemplateConfig is removed
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
// import org.springframework.web.client.HttpClientErrorException; // Removed
// import org.springframework.web.client.RestTemplate; // Removed

import java.util.Map;
// import org.springframework.context.annotation.Bean; // Removed if RestTemplateConfig is removed


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final UserServiceClient userServiceClient;
    // private final RestTemplate restTemplate; // Removed
    private final KeycloakClient keycloakClient; // Added

    // @Value("${keycloak.token-uri}") // Removed - now used in KeycloakClient
    // private String keycloakTokenUri;

    @Value("${keycloak.client-id}")
    private String keycloakClientId;

    @Value("${keycloak.client-secret}")
    private String keycloakClientSecret;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            // 1. Validate credentials with user-service (remains the same)
            boolean isValid = userServiceClient.validateCredentials(request);
            logger.debug("Credentials validation for {}: {}", request.getEmail(), isValid);

            if (!isValid) {
                logger.warn("Invalid credentials for user: {}", request.getEmail());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 2. If credentials are valid, request a token from Keycloak using ROPC grant
            // HttpHeaders headers = new HttpHeaders(); // Not directly needed for Feign like this
            // headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED); // Handled by Feign client's 'consumes'

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("client_id", keycloakClientId);
            map.add("client_secret", keycloakClientSecret);
            map.add("grant_type", "password");
            map.add("username", request.getEmail());
            map.add("password", request.getPassword());
            map.add("scope", "openid profile email"); // Request desired scopes

            // HttpEntity<MultiValueMap<String, String>> keycloakRequest = new HttpEntity<>(map, headers); // Not needed for Feign
            logger.info("Requesting token from Keycloak for user: {}", request.getEmail());
            logger.debug("Keycloak request form params being sent: {}", map); // Added for debugging
            
            // ResponseEntity<Map> keycloakResponse = restTemplate.postForEntity(keycloakTokenUri, keycloakRequest, Map.class); // Replaced
            ResponseEntity<Map<String, Object>> keycloakResponse = keycloakClient.getToken(map); // Changed to use Feign client

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

        } catch (FeignException.FeignClientException e) { // Changed from HttpClientErrorException
            logger.error("FeignClientException during Keycloak token request for user {}: Status {} - Response Body: {}", request.getEmail(), e.status(), e.contentUTF8(), e);
            return ResponseEntity.status(e.status()).build();
        } catch (FeignException e) { // Catch other Feign related errors (e.g., server errors, connectivity)
            logger.error("FeignException during Keycloak token request for user {}: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
         catch (Exception e) {
            logger.error("Exception during login for user {}: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

// Remove RestTemplateConfig if RestTemplate is no longer used elsewhere in this service
// @Component
// class RestTemplateConfig {
//     @Bean
//     public RestTemplate restTemplate() {
//         return new RestTemplate();
//     }
// }