package com.librarysystem.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarysystem.userservice.config.GatewayValidationFilter;
import com.librarysystem.userservice.config.RoleExtractionFilter;
import com.librarysystem.userservice.config.TestSecurityConfig; // Assuming you have this
import com.librarysystem.userservice.dto.LoginRequest;
import com.librarysystem.userservice.dto.UserCreateRequest;
import com.librarysystem.userservice.entity.User;
import com.librarysystem.userservice.entity.UserRole;
import com.librarysystem.userservice.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class) // Import test-specific security configuration
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GatewayValidationFilter gatewayValidationFilter;

    @MockBean
    private RoleExtractionFilter roleExtractionFilter;

    private UserCreateRequest validUserCreateRequest;
    private User userEntity;
    private String userId;
    private final String validationFailedMessage = "Input validation failed for one or more fields.";
    
    private final String gatewaySecret = "test-user-service-secret"; // Example secret

    private final String MEMBER_ROLE = "MEMBER";
    private final String LIBRARIAN_ROLE = "LIBRARIAN";
    private final String ADMIN_ROLE = "ADMIN";

    private final String MEMBER_USER_ID = "member-user-id";
    private final String LIBRARIAN_USER_ID = "librarian-user-id";
    private final String ADMIN_USER_ID = "admin-user-id";
    private final String ANONYMOUS_ID = "anonymous-test-user";


    @BeforeEach
    void setUp() throws ServletException, IOException {
        userId = UUID.randomUUID().toString(); // Keep for specific user entity ID

        validUserCreateRequest = new UserCreateRequest();
        validUserCreateRequest.setFirstName("Test");
        validUserCreateRequest.setLastName("User");
        validUserCreateRequest.setEmail("test.user@example.com");
        validUserCreateRequest.setPassword("Password123!");
        validUserCreateRequest.setRole(UserRole.MEMBER);

        userEntity = new User();
        userEntity.setId(userId); // Use the generated userId for the main test entity
        userEntity.setFirstName("Test");
        userEntity.setLastName("User");
        userEntity.setEmail("test.user@example.com");
        userEntity.setPassword("encodedPassword");
        userEntity.setRole(UserRole.MEMBER);
        userEntity.setActive(true);
        userEntity.setCreatedAt(LocalDateTime.now());

        // Mock GatewayValidationFilter to proceed
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(gatewayValidationFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        // Mock RoleExtractionFilter to simulate role extraction and SecurityContext setup
        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);

            String rolesHeader = request.getHeader("X-User-Roles");
            String userIdHeader = request.getHeader("X-User-Id");

            String principalName = (userIdHeader != null && !userIdHeader.isEmpty()) ? userIdHeader : ANONYMOUS_ID;
            List<GrantedAuthority> authorities = new ArrayList<>();

            if (rolesHeader != null && !rolesHeader.isEmpty()) {
                authorities = Arrays.stream(rolesHeader.split(","))
                        .map(role -> "ROLE_" + role.trim().toUpperCase())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principalName, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            try {
                chain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
            return null;
        }).when(roleExtractionFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));
    }

    // Helper methods for performing requests
    private ResultActions performPostWithRole(String url, Object body, String userId, String roles) throws Exception {
        return mockMvc.perform(post(url)
                .header("X-Gateway-Secret", gatewaySecret)
                .header("X-User-Id", userId)
                .header("X-User-Roles", roles)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions performPostWithoutRole(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .header("X-Gateway-Secret", gatewaySecret)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions performGetWithRole(String url, String userId, String roles) throws Exception {
        return mockMvc.perform(get(url)
                .header("X-Gateway-Secret", gatewaySecret)
                .header("X-User-Id", userId)
                .header("X-User-Roles", roles)
                .accept(MediaType.APPLICATION_JSON));
    }
    
    private ResultActions performGetWithoutRole(String url) throws Exception {
        return mockMvc.perform(get(url)
                .header("X-Gateway-Secret", gatewaySecret)
                .accept(MediaType.APPLICATION_JSON));
    }

    private ResultActions performDeleteWithRole(String url, String userId, String roles) throws Exception {
        return mockMvc.perform(delete(url)
                .header("X-Gateway-Secret", gatewaySecret)
                .header("X-User-Id", userId)
                .header("X-User-Roles", roles));
    }

    // --- POST /api/users (Create User) ---

    @Test
    @DisplayName("POST /api/users - Create User - Success")
    void createUser_whenValidRequest_shouldReturnCreatedUserDTO() throws Exception {
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(userEntity);

        performPostWithoutRole("/api/users", validUserCreateRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(userId)))
                .andExpect(jsonPath("$.email", is(validUserCreateRequest.getEmail())))
                .andExpect(jsonPath("$.firstName", is(validUserCreateRequest.getFirstName())))
                .andExpect(jsonPath("$.role", is(UserRole.MEMBER.name())))
                .andExpect(jsonPath("$.active", is(true)));

        verify(userService).createUser(any(UserCreateRequest.class));
    }

    @Test
    @DisplayName("POST /api/users - Create User - Email Already Exists (Service Exception)")
    void createUser_whenEmailExists_shouldReturnConflict() throws Exception {
        String errorMessage = "Email already exists";
        when(userService.createUser(any(UserCreateRequest.class)))
                .thenThrow(new RuntimeException(errorMessage));

        performPostWithoutRole("/api/users", validUserCreateRequest)
                .andExpect(status().isConflict()) // Expect 409 Conflict
                .andExpect(jsonPath("$.message", is(errorMessage)));

        verify(userService).createUser(any(UserCreateRequest.class));
    }

    @Test
    @DisplayName("POST /api/users - Create User - Invalid Request Body (All @NotBlank Fields Missing)")
    void createUser_whenInvalidRequestBody_AllNotBlankFieldsMissing_shouldReturnBadRequest() throws Exception {
        UserCreateRequest invalidRequest = new UserCreateRequest();
        invalidRequest.setFirstName(null);
        invalidRequest.setLastName(null);
        invalidRequest.setEmail(null);
        invalidRequest.setPassword(null);

        performPostWithoutRole("/api/users", invalidRequest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(validationFailedMessage)))
                .andExpect(jsonPath("$.details", hasSize(4)))
                .andExpect(jsonPath("$.details", containsInAnyOrder(
                        "firstName: First name is required",
                        "lastName: Last name is required",
                        "email: Email is required",
                        "password: Password is required"
                )));

        verify(userService, never()).createUser(any());
    }

    @Test
    @DisplayName("POST /api/users - Create User - Invalid Email Format")
    void createUser_whenInvalidEmailFormat_shouldReturnBadRequest() throws Exception {
        UserCreateRequest requestWithInvalidEmail = new UserCreateRequest();
        requestWithInvalidEmail.setFirstName("Test");
        requestWithInvalidEmail.setLastName("User");
        requestWithInvalidEmail.setEmail("notanemail");
        requestWithInvalidEmail.setPassword("Password123!");
        requestWithInvalidEmail.setRole(UserRole.MEMBER);

        performPostWithoutRole("/api/users", requestWithInvalidEmail)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(validationFailedMessage)))
                .andExpect(jsonPath("$.details", hasSize(1)))
                .andExpect(jsonPath("$.details[0]", is("email: Invalid email format")));

        verify(userService, never()).createUser(any());
    }

    // --- DELETE /api/users/{id} (Deactivate User) ---

    @Test
    @DisplayName("DELETE /api/users/{id} - Deactivate User - Success (Admin Role)")
    void deactivateUser_whenUserExistsAndAdmin_shouldReturnOk() throws Exception {
        doNothing().when(userService).deactivateUser(userId);

        performDeleteWithRole("/api/users/" + userId, ADMIN_USER_ID, ADMIN_ROLE)
                .andExpect(status().isOk());

        verify(userService).deactivateUser(userId);
    }

    @Test
    @DisplayName("DELETE /api/users/{id} - Deactivate User - Forbidden (Non-Admin Role)")
    void deactivateUser_whenUserExistsAndNotAdmin_shouldReturnForbidden() throws Exception {
        performDeleteWithRole("/api/users/" + userId, MEMBER_USER_ID, MEMBER_ROLE)
                .andExpect(status().isForbidden());

        verify(userService, never()).deactivateUser(anyString());
    }


    @Test
    @DisplayName("DELETE /api/users/{id} - Deactivate User - User Not Found (Service Exception, Admin Role)")
    void deactivateUser_whenUserNotFoundAndAdmin_shouldReturnNotFound() throws Exception {
        String errorMessage = "User not found with ID: " + userId;
        doThrow(new RuntimeException(errorMessage)).when(userService).deactivateUser(userId);

        performDeleteWithRole("/api/users/" + userId, ADMIN_USER_ID, ADMIN_ROLE)
                .andExpect(status().isNotFound()) // Expect 404 Not Found
                .andExpect(jsonPath("$.message", is(errorMessage)));

        verify(userService).deactivateUser(userId);
    }

    // --- GET /api/users/{id} (Get User By Id) ---

    @Test
    @DisplayName("GET /api/users/{id} - Get User By Id - Success (Admin Role)")
    void getUserById_whenAdminRequests_shouldReturnUserDTO() throws Exception {
        when(userService.getUserById(userId)).thenReturn(userEntity);

        performGetWithRole("/api/users/" + userId, ADMIN_USER_ID, ADMIN_ROLE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(userId)))
                .andExpect(jsonPath("$.email", is(userEntity.getEmail())));

        verify(userService).getUserById(userId);
    }

    @Test
    @DisplayName("GET /api/users/{id} - Get User By Id - Success (Requesting Self)")
    void getUserById_whenUserRequestsSelf_shouldReturnUserDTO() throws Exception {
        when(userService.getUserById(userId)).thenReturn(userEntity); // userEntity.id is 'userId'

        performGetWithRole("/api/users/" + userId, userId, MEMBER_ROLE) // Requesting self
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(userId)));

        verify(userService).getUserById(userId);
    }

    @Test
    @DisplayName("GET /api/users/{id} - Get User By Id - Forbidden (Other User, Not Admin)")
    void getUserById_whenUserRequestsOtherAndNotAdmin_shouldReturnForbidden() throws Exception {
        String otherUserId = UUID.randomUUID().toString();

        performGetWithRole("/api/users/" + otherUserId, MEMBER_USER_ID, MEMBER_ROLE)
                .andExpect(status().isForbidden()); 

        verify(userService, never()).getUserById(otherUserId);
    }

    @Test
    @DisplayName("GET /api/users/{id} - Get User By Id - User Not Found (Service Exception, Requesting Self)")
    void getUserById_whenUserNotFound_shouldReturnNotFound() throws Exception {
        String errorMessage = "User not found with ID: " + userId;
        when(userService.getUserById(userId)).thenThrow(new RuntimeException(errorMessage));

        performGetWithRole("/api/users/" + userId, userId, MEMBER_ROLE) // Requesting self
                .andExpect(status().isNotFound()) // Expect 404 Not Found
                .andExpect(jsonPath("$.message", is(errorMessage)));

        verify(userService).getUserById(userId);
    }

    // --- GET /api/users/email/{email} (Get User By Email) ---

    @Test
    @DisplayName("GET /api/users/email/{email} - Get User By Email - Success (Any Authenticated User)")
    void getUserByEmail_whenUserExists_shouldReturnUserDTO() throws Exception {
        when(userService.getUserByEmail(userEntity.getEmail())).thenReturn(userEntity);

        performGetWithRole("/api/users/email/" + userEntity.getEmail(), MEMBER_USER_ID, MEMBER_ROLE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(userEntity.getEmail())));

        verify(userService).getUserByEmail(userEntity.getEmail());
    }

    @Test
    @DisplayName("GET /api/users/email/{email} - Get User By Email - User Not Found (Service Exception, Any Authenticated User)")
    void getUserByEmail_whenUserNotFound_shouldReturnNotFound() throws Exception {
        String nonExistentEmail = "nouser@example.com";
        String errorMessage = "User not found with email: " + nonExistentEmail;
        when(userService.getUserByEmail(nonExistentEmail)).thenThrow(new RuntimeException(errorMessage));

        performGetWithRole("/api/users/email/" + nonExistentEmail, MEMBER_USER_ID, MEMBER_ROLE)
                .andExpect(status().isNotFound()) // Expect 404 Not Found
                .andExpect(jsonPath("$.message", is(errorMessage)));

        verify(userService).getUserByEmail(nonExistentEmail);
    }

    // --- POST /api/users/validate (Validate Credentials) ---

    @Test
    @DisplayName("POST /api/users/validate - Validate Credentials - Valid and Active User")
    void validateCredentials_whenValidAndActive_shouldReturnTrue() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(userEntity.getEmail());
        loginRequest.setPassword("RawPassword123!");
        userEntity.setActive(true);

        when(userService.getUserByEmail(userEntity.getEmail())).thenReturn(userEntity);
        when(userService.validatePassword("RawPassword123!", userEntity.getPassword())).thenReturn(true);

        performPostWithoutRole("/api/users/validate", loginRequest)
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(userService).getUserByEmail(userEntity.getEmail());
        verify(userService).validatePassword("RawPassword123!", userEntity.getPassword());
    }

    @Test
    @DisplayName("POST /api/users/validate - Validate Credentials - Invalid Password")
    void validateCredentials_whenInvalidPassword_shouldReturnFalse() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(userEntity.getEmail());
        loginRequest.setPassword("WrongPassword");
        userEntity.setActive(true);

        when(userService.getUserByEmail(userEntity.getEmail())).thenReturn(userEntity);
        when(userService.validatePassword("WrongPassword", userEntity.getPassword())).thenReturn(false);

        performPostWithoutRole("/api/users/validate", loginRequest)
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(userService).getUserByEmail(userEntity.getEmail());
        verify(userService).validatePassword("WrongPassword", userEntity.getPassword());
    }

    @Test
    @DisplayName("POST /api/users/validate - Validate Credentials - User Not Found")
    void validateCredentials_whenUserNotFound_shouldReturnFalse() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nouser@example.com");
        loginRequest.setPassword("Password123!");

        when(userService.getUserByEmail("nouser@example.com")).thenThrow(new RuntimeException("User not found"));

        performPostWithoutRole("/api/users/validate", loginRequest)
                .andExpect(status().isOk())
                .andExpect(content().string("false")); 

        verify(userService).getUserByEmail("nouser@example.com");
        verify(userService, never()).validatePassword(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /api/users/validate - Validate Credentials - User Inactive")
    void validateCredentials_whenUserInactive_shouldReturnFalse() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(userEntity.getEmail());
        loginRequest.setPassword("RawPassword123!");
        userEntity.setActive(false);

        when(userService.getUserByEmail(userEntity.getEmail())).thenReturn(userEntity);
        when(userService.validatePassword("RawPassword123!", userEntity.getPassword())).thenReturn(true);

        performPostWithoutRole("/api/users/validate", loginRequest)
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(userService).getUserByEmail(userEntity.getEmail());
        verify(userService).validatePassword("RawPassword123!", userEntity.getPassword());
    }

    @Test
    @DisplayName("POST /api/users/validate - Validate Credentials - Invalid Request Body (Both Fields Missing)")
    void validateCredentials_whenInvalidRequestBody_BothFieldsMissing_shouldReturnBadRequest() throws Exception {
        LoginRequest invalidLoginRequest = new LoginRequest(); 

        performPostWithoutRole("/api/users/validate", invalidLoginRequest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(validationFailedMessage)))
                .andExpect(jsonPath("$.details", hasSize(2)))
                .andExpect(jsonPath("$.details", containsInAnyOrder(
                        "email: Email is required",
                        "password: Password is required"
                )));

        verify(userService, never()).getUserByEmail(anyString());
        verify(userService, never()).validatePassword(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /api/users/validate - Validate Credentials - Invalid Email Format in Request")
    void validateCredentials_whenInvalidEmailFormatInRequest_shouldReturnBadRequest() throws Exception {
        LoginRequest loginRequestWithInvalidEmail = new LoginRequest();
        loginRequestWithInvalidEmail.setEmail("notavalidemail");
        loginRequestWithInvalidEmail.setPassword("ValidPassword123!");

        performPostWithoutRole("/api/users/validate", loginRequestWithInvalidEmail)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is(validationFailedMessage)))
                .andExpect(jsonPath("$.details", hasSize(1)))
                .andExpect(jsonPath("$.details[0]", is("email: Invalid email format")));

        verify(userService, never()).getUserByEmail(anyString());
        verify(userService, never()).validatePassword(anyString(), anyString());
    }
}