package com.librarysystem.authservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO representing the response from Keycloak's token endpoint.
 * Contains access and refresh tokens, expiration info, and related metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakTokenResponse {

    /**
     * The access token issued by Keycloak.
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * The number of seconds until the access token expires.
     */
    @JsonProperty("expires_in")
    private Integer expiresIn;

    /**
     * The number of seconds until the refresh token expires.
     */
    @JsonProperty("refresh_expires_in")
    private Integer refreshExpiresIn;

    /**
     * The refresh token issued by Keycloak.
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * The type of the token (usually "Bearer").
     */
    @JsonProperty("token_type")
    private String tokenType;

    /**
     * Not-before policy value.
     */
    @JsonProperty("not-before-policy")
    private Integer notBeforePolicy;

    /**
     * The session state identifier.
     */
    @JsonProperty("session_state")
    private String sessionState;

    /**
     * The scope of the issued token.
     */
    @JsonProperty("scope")
    private String scope;
}