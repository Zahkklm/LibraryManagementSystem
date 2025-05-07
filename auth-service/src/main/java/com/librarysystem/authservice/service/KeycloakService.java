package com.librarysystem.authservice.service;

import com.librarysystem.authservice.dto.KeycloakTokenResponse;
import com.librarysystem.authservice.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class KeycloakService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}") // Changed from keycloak.resource
    private String clientId;

    @Value("${keycloak.client-secret}") // Changed from keycloak.credentials.secret
    private String clientSecret;

    public KeycloakTokenResponse getToken(LoginRequest loginRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "password");
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("username", loginRequest.getEmail());
        map.add("password", loginRequest.getPassword());

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<KeycloakTokenResponse> response = restTemplate.postForEntity(
                keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token",
                request,
                KeycloakTokenResponse.class);

        return response.getBody();
    }

    public KeycloakTokenResponse refreshToken(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "refresh_token");
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<KeycloakTokenResponse> response = restTemplate.postForEntity(
                keycloakServerUrl + "/realms/" + realm + "/protocol/openid-connect/token",
                request,
                KeycloakTokenResponse.class);

        return response.getBody();
    }
}