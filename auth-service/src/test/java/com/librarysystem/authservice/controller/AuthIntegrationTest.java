package com.librarysystem.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.librarysystem.authservice.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0) // This starts WireMock on a random port
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Use the system property set by @AutoConfigureWireMock to get the port
        String port = System.getProperty("wiremock.server.port");
        registry.add("user-service.url", () -> "http://localhost:" + port);
    }

    @Test
    void login_withInvalidCredentials_returnsUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest("invalid@example.com", "wrongpassword");

        // Simulate user-service returning false for invalid credentials
        WireMock.stubFor(WireMock.post("/api/auth/login")
            .willReturn(WireMock.aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("false")));

        mockMvc.perform(post("/api/auth/login")
                .header("X-Gateway-Secret", "M3lT#9fGz!8qP@Z^rKw!5xL7e2Bj&T0v")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}