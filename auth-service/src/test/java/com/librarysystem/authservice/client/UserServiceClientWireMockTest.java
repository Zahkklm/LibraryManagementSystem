package com.librarysystem.authservice.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.librarysystem.authservice.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureWireMock(port = 0) // random port
@EnableFeignClients(clients = UserServiceClient.class)
class UserServiceClientWireMockTest {

    @Autowired
    private UserServiceClient userServiceClient;

    @BeforeEach
    void setup() {
        WireMock.reset();
    }

    @Test
    void validateCredentials_returnsTrue_whenUserServiceSaysSo() {
        WireMock.stubFor(WireMock.post("/api/users/validate")
                .willReturn(WireMock.aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        boolean result = userServiceClient.validateCredentials(new LoginRequest("test@example.com", "pass"));
        assertTrue(result);
    }
}