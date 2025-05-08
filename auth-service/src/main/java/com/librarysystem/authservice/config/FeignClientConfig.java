package com.librarysystem.authservice.config;

import com.librarysystem.authservice.client.KeycloakClient;
import feign.FeignException;
import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class FeignClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(FeignClientConfig.class);
    private final KeycloakClient keycloakClient;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${gateway.secret}") 
    private String gatewaySecretHeaderValue;

    private String cachedServiceToken;
    private long tokenExpiryTimeMillis;
    private static final long TOKEN_EXPIRY_BUFFER_SECONDS = 30; // Fetch new token 30s before actual expiry
    private static final String GATEWAY_SECRET_HEADER_NAME = "X-Gateway-Secret";


    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // Add the X-Gateway-Secret header for inter-service communication
            // to satisfy the GatewayValidationFilter in downstream services like user-service.
            if (gatewaySecretHeaderValue != null && !gatewaySecretHeaderValue.isEmpty()) {
                requestTemplate.header(GATEWAY_SECRET_HEADER_NAME, gatewaySecretHeaderValue);
                logger.debug("Added {} header to Feign request to {}", GATEWAY_SECRET_HEADER_NAME, requestTemplate.url());
            } else {
                logger.warn("{} is not configured. Header will not be added for Feign requests.", GATEWAY_SECRET_HEADER_NAME);
            }

            // Existing logic for service account token (if needed for other purposes)
            String serviceToken = getServiceToken();
            if (serviceToken != null) {
                requestTemplate.header("Authorization", "Bearer " + serviceToken);
            } else {
                logger.warn("Service token is null. Authorization header will not be added for UserServiceClient.");
            }
        };
    }

    private synchronized String getServiceToken() {
        if (cachedServiceToken != null && System.currentTimeMillis() < tokenExpiryTimeMillis) {
            logger.debug("Using cached service token for UserServiceClient.");
            return cachedServiceToken;
        }

        logger.info("Fetching new service token from Keycloak for UserServiceClient.");
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        // formData.add("scope", "internal_service_scope"); // Optional: Add if you have specific scopes for service accounts

        try {
            ResponseEntity<Map<String, Object>> response = keycloakClient.getClientCredentialsToken(formData);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                cachedServiceToken = (String) responseBody.get("access_token");
                Object expiresInObj = responseBody.get("expires_in");

                if (cachedServiceToken != null && expiresInObj instanceof Number) {
                    long expiresInSeconds = ((Number) expiresInObj).longValue();
                    tokenExpiryTimeMillis = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expiresInSeconds) - TimeUnit.SECONDS.toMillis(TOKEN_EXPIRY_BUFFER_SECONDS);
                    logger.info("Successfully fetched new service token for UserServiceClient. Expires in approx {} seconds.", expiresInSeconds);
                    return cachedServiceToken;
                } else {
                    logger.error("Keycloak response for service token did not contain access_token or valid expires_in. Response: {}", responseBody);
                    clearCachedToken();
                    return null;
                }
            } else {
                logger.error("Failed to fetch service token from Keycloak. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                clearCachedToken();
                return null;
            }
        } catch (FeignException e) {
            logger.error("FeignException while fetching service token for UserServiceClient: {} - Status: {} - Body: {}", e.getMessage(), e.status(), e.contentUTF8(), e);
            clearCachedToken();
            return null;
        } catch (Exception e) {
            logger.error("Unexpected exception while fetching service token for UserServiceClient: {}", e.getMessage(), e);
            clearCachedToken();
            return null;
        }
    }

    private void clearCachedToken() {
        this.cachedServiceToken = null;
        this.tokenExpiryTimeMillis = 0;
    }
}