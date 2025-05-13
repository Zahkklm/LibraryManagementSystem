package com.librarysystem.authservice.service;

import com.librarysystem.authservice.dto.KeycloakTokenResponse;
import com.librarysystem.authservice.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KeycloakServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private KeycloakService keycloakService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Inject test values for @Value fields using reflection
        setField(keycloakService, "keycloakServerUrl", "http://localhost:8090");
        setField(keycloakService, "realm", "library");
        setField(keycloakService, "clientId", "test-client");
        setField(keycloakService, "clientSecret", "test-secret");
        // Replace the real RestTemplate with the mock
        setField(keycloakService, "restTemplate", restTemplate);
    }

    @Test
    void getToken_shouldReturnTokenResponse() {
        LoginRequest loginRequest = new LoginRequest("user@example.com", "password");
        KeycloakTokenResponse tokenResponse = new KeycloakTokenResponse();
        tokenResponse.setAccessToken("access-token");
        tokenResponse.setRefreshToken("refresh-token");

        ResponseEntity<KeycloakTokenResponse> responseEntity =
                new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(KeycloakTokenResponse.class)
        )).thenReturn(responseEntity);

        KeycloakTokenResponse result = keycloakService.getToken(loginRequest);

        assertNotNull(result);
        assertEquals("access-token", result.getAccessToken());
        assertEquals("refresh-token", result.getRefreshToken());
    }

    @Test
    void refreshToken_shouldReturnTokenResponse() {
        String refreshToken = "refresh-token";
        KeycloakTokenResponse tokenResponse = new KeycloakTokenResponse();
        tokenResponse.setAccessToken("new-access-token");
        tokenResponse.setRefreshToken("new-refresh-token");

        ResponseEntity<KeycloakTokenResponse> responseEntity =
                new ResponseEntity<>(tokenResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(
                anyString(),
                any(HttpEntity.class),
                eq(KeycloakTokenResponse.class)
        )).thenReturn(responseEntity);

        KeycloakTokenResponse result = keycloakService.refreshToken(refreshToken);

        assertNotNull(result);
        assertEquals("new-access-token", result.getAccessToken());
        assertEquals("new-refresh-token", result.getRefreshToken());
    }

    // Utility for setting private fields via reflection in tests
    static void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
