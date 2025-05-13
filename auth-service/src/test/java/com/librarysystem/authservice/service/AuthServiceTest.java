package com.librarysystem.authservice.service;

import com.librarysystem.authservice.client.KeycloakClient;
import com.librarysystem.authservice.client.UserServiceClient;
import com.librarysystem.authservice.dto.JwtResponse;
import com.librarysystem.authservice.dto.LoginRequest;
import com.librarysystem.authservice.dto.UserCreateRequest;
import com.librarysystem.authservice.dto.UserDTO;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private KeycloakClient keycloakClient;

    @InjectMocks
    private AuthService authService;

    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        setField(authService, "keycloakClientId", "test-client");
        setField(authService, "keycloakClientSecret", "test-secret");

        // Setup a reusable UserDTO
        userDTO = new UserDTO();
        userDTO.setId("id");
        userDTO.setFirstName("Test");
        userDTO.setLastName("User");
        userDTO.setEmail("test@example.com");
        userDTO.setRole("MEMBER");
    }

    @Test
    void login_successful() {
        LoginRequest request = new LoginRequest("test@example.com", "password");
        when(userServiceClient.validateCredentials(request)).thenReturn(true);

        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("access_token", "jwt-token");
        when(keycloakClient.getToken(any(LinkedMultiValueMap.class)))
                .thenReturn(new ResponseEntity<>(tokenMap, HttpStatus.OK));

        ResponseEntity<JwtResponse> response = authService.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("jwt-token", response.getBody().getToken());
    }

    @Test
    void login_invalidCredentials() {
        LoginRequest request = new LoginRequest("test@example.com", "wrongpass");
        when(userServiceClient.validateCredentials(request)).thenReturn(false);

        ResponseEntity<JwtResponse> response = authService.login(request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void login_keycloakReturnsNoAccessToken_returnsInternalServerError() {
        LoginRequest request = new LoginRequest("test@example.com", "password");
        when(userServiceClient.validateCredentials(request)).thenReturn(true);

        Map<String, Object> tokenMap = new HashMap<>(); // No "access_token"
        when(keycloakClient.getToken(any(LinkedMultiValueMap.class)))
                .thenReturn(new ResponseEntity<>(tokenMap, HttpStatus.OK));

        ResponseEntity<JwtResponse> response = authService.login(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void login_keycloakReturnsNonOkStatus_returnsInternalServerError() {
        LoginRequest request = new LoginRequest("test@example.com", "password");
        when(userServiceClient.validateCredentials(request)).thenReturn(true);

        Map<String, Object> tokenMap = new HashMap<>();
        when(keycloakClient.getToken(any(LinkedMultiValueMap.class)))
                .thenReturn(new ResponseEntity<>(tokenMap, HttpStatus.BAD_REQUEST));

        ResponseEntity<JwtResponse> response = authService.login(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void login_feignClientException_returnsClientStatus() {
        LoginRequest request = new LoginRequest("test@example.com", "password");
        when(userServiceClient.validateCredentials(request)).thenReturn(true);

        FeignException.FeignClientException feignEx =
                mock(FeignException.FeignClientException.class);
        when(feignEx.status()).thenReturn(401);
        when(feignEx.contentUTF8()).thenReturn("Unauthorized");
        when(keycloakClient.getToken(any(LinkedMultiValueMap.class))).thenThrow(feignEx);

        ResponseEntity<JwtResponse> response = authService.login(request);

        assertEquals(401, response.getStatusCodeValue());
        assertNull(response.getBody());
    }

    @Test
    void login_feignException_returnsInternalServerError() {
        LoginRequest request = new LoginRequest("test@example.com", "password");
        when(userServiceClient.validateCredentials(request)).thenReturn(true);

        when(keycloakClient.getToken(any(LinkedMultiValueMap.class)))
                .thenThrow(mock(FeignException.class));

        ResponseEntity<JwtResponse> response = authService.login(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void login_unexpectedException_returnsInternalServerError() {
        LoginRequest request = new LoginRequest("test@example.com", "password");
        when(userServiceClient.validateCredentials(request)).thenThrow(new RuntimeException("Unexpected"));

        ResponseEntity<JwtResponse> response = authService.login(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void register_successful() {
        UserCreateRequest createRequest = new UserCreateRequest();
        createRequest.setFirstName("Test");
        createRequest.setLastName("User");
        createRequest.setEmail("test@example.com");
        createRequest.setPassword("password");
        when(userServiceClient.createUser(createRequest)).thenReturn(userDTO);

        // Mock login after registration
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password");
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("access_token", "jwt-token");
        when(userServiceClient.validateCredentials(loginRequest)).thenReturn(true);
        when(keycloakClient.getToken(any(LinkedMultiValueMap.class)))
                .thenReturn(new ResponseEntity<>(tokenMap, HttpStatus.OK));

        ResponseEntity<?> response = authService.register(createRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals(userDTO, body.get("user"));
        assertEquals("jwt-token", body.get("token"));
    }

    @Test
    void register_conflict() {
        UserCreateRequest createRequest = new UserCreateRequest();
        createRequest.setFirstName("Test");
        createRequest.setLastName("User");
        createRequest.setEmail("test@example.com");
        createRequest.setPassword("password");
        when(userServiceClient.createUser(createRequest)).thenThrow(FeignException.Conflict.class);

        ResponseEntity<?> response = authService.register(createRequest);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Email already exists", response.getBody());
    }

    @Test
    void register_unexpectedException_returnsInternalServerError() {
        UserCreateRequest createRequest = new UserCreateRequest();
        createRequest.setFirstName("Test");
        createRequest.setLastName("User");
        createRequest.setEmail("test@example.com");
        createRequest.setPassword("password");
        when(userServiceClient.createUser(createRequest)).thenThrow(new RuntimeException("Unexpected"));

        ResponseEntity<?> response = authService.register(createRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Registration failed", response.getBody());
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