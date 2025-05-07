package com.librarysystem.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign client for interacting with Keycloak's token endpoint.
 * Provides methods for obtaining tokens using different OAuth2 grant types.
 */
@FeignClient(name = "keycloak-client", url = "${keycloak.token-uri}")
public interface KeycloakClient {

    /**
     * Obtains an access token using the Resource Owner Password Credentials (ROPC) grant.
     *
     * @param formParams The form parameters required for the ROPC grant, including:
     *                   - client_id
     *                   - client_secret
     *                   - grant_type (password)
     *                   - username
     *                   - password
     *                   - scope (optional)
     * @return A ResponseEntity containing the token response from Keycloak.
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Map<String, Object>> getToken(@RequestBody MultiValueMap<String, String> formParams);

    /**
     * Obtains an access token using the Client Credentials grant.
     *
     * @param formParams The form parameters required for the Client Credentials grant, including:
     *                   - client_id
     *                   - client_secret
     *                   - grant_type (client_credentials)
     *                   - scope (optional)
     * @return A ResponseEntity containing the token response from Keycloak.
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<Map<String, Object>> getClientCredentialsToken(@RequestBody MultiValueMap<String, String> formParams);
}