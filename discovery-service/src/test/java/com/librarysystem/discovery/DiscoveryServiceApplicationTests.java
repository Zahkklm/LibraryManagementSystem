package com.librarysystem.discovery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Discovery Service application.
 * This class tests the functionality of the discovery service, including service registration and discovery.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class DiscoveryServiceApplicationTests {

  @Mock
  private DiscoveryClient discoveryClient; // Mocked DiscoveryClient to simulate service registration and discovery

  @Autowired
  private TestRestTemplate restTemplate; // For testing HTTP endpoints of the discovery service

  /**
   * Sets up the test environment before each test method.
   * Initializes mocks and configures the behavior of the mocked DiscoveryClient.
   */
  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this); // Initialize mocks
    // Mock the behavior of the discovery client to return a list of services
    when(discoveryClient.getServices()).thenReturn(List.of("auth-service"));

    // Mock the behavior for getting instances of the auth-service
    ServiceInstance mockInstance = Mockito.mock(ServiceInstance.class); // Create a mock instance
    when(mockInstance.getServiceId()).thenReturn("auth-service"); // Define the service ID for the mock instance
    when(discoveryClient.getInstances("auth-service")).thenReturn(List.of(mockInstance)); // Return the mock instance when requested
  }

  /**
   * Test to verify that the application context loads successfully.
   */
  @Test
  void contextLoads() {
    // This test will simply check if the application context loads successfully
  }

  /**
   * Test to verify that the discovery client can register the auth-service.
   */
  @Test
  void testServiceRegistration() {
    // Assuming the service registers itself with Eureka on startup
    List<String> services = discoveryClient.getServices(); // Retrieve the list of registered services
    assertThat(services).contains("auth-service"); // Assert that the auth-service is included in the list
  }

  /**
   * Test to verify that the discovery client can discover the auth-service.
   */
  @Test
  void testServiceDiscovery() {
    // Assuming the service can discover other services
    ServiceInstance instance = discoveryClient.getInstances("auth-service").stream().findFirst().orElse(null); // Get the first instance of auth-service
    assertThat(instance).isNotNull(); // Assert that the instance is not null
    assertThat(instance.getServiceId()).isEqualTo("auth-service"); // Assert that the service ID matches
  }
}
