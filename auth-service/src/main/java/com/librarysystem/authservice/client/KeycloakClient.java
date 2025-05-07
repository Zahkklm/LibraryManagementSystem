package com.librarysystem.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody; // Ensure this is org.springframework.web.bind.annotation.RequestBody

import java.util.Map;

@FeignClient(name = "keycloak-client", url = "${keycloak.token-uri}")
public interface KeycloakClient {

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE) // Removed consumes attribute
    ResponseEntity<Map<String, Object>> getToken(@RequestBody MultiValueMap<String, String> formParams);

    // Added for Client Credentials Grant
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE) // Removed consumes attribute
    ResponseEntity<Map<String, Object>> getClientCredentialsToken(@RequestBody MultiValueMap<String, String> formParams);
}