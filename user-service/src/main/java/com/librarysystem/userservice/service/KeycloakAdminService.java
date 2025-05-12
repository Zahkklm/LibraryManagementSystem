package com.librarysystem.userservice.service;

import com.librarysystem.userservice.dto.UserCreateRequest;
import com.librarysystem.userservice.entity.UserRole;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class KeycloakAdminService {

    private static final Logger logger = LoggerFactory.getLogger(KeycloakAdminService.class);
    private final Keycloak keycloakAdminClient;

    @Value("${keycloak.target-realm}")
    private String targetRealm;

    /**
     * Creates a user in Keycloak and assigns a role.
     * Returns the Keycloak user ID (UUID) if successful.
     */
    public String createKeycloakUser(UserCreateRequest userRequest, String plainPassword) {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(userRequest.getEmail());
        userRepresentation.setEmail(userRequest.getEmail());
        userRepresentation.setFirstName(userRequest.getFirstName());
        userRepresentation.setLastName(userRequest.getLastName());
        userRepresentation.setEnabled(true);
        userRepresentation.setEmailVerified(true);

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(plainPassword);
        credential.setTemporary(false);
        userRepresentation.setCredentials(Collections.singletonList(credential));

        RealmResource realmResource = keycloakAdminClient.realm(targetRealm);
        UsersResource usersResource = realmResource.users();

        Response response = null;
        try {
            logger.info("Creating user {} in Keycloak realm {}", userRequest.getEmail(), targetRealm);
            response = usersResource.create(userRepresentation);

            if (response.getStatus() == 201) {
                String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
                logger.info("User {} created in Keycloak with ID: {}", userRequest.getEmail(), userId);

                // Assign realm role
                UserRole roleToAssign = userRequest.getRole() != null ? userRequest.getRole() : UserRole.MEMBER;
                RoleRepresentation realmRole = realmResource.roles().get(roleToAssign.name()).toRepresentation();
                if (realmRole != null) {
                    usersResource.get(userId).roles().realmLevel().add(Collections.singletonList(realmRole));
                    logger.info("Assigned role {} to user {} in Keycloak", roleToAssign.name(), userRequest.getEmail());
                } else {
                    logger.warn("Role {} not found in Keycloak realm {}. User {} created without this role.",
                            roleToAssign.name(), targetRealm, userRequest.getEmail());
                }
                return userId;
            } else {
                String errorMessage = response.readEntity(String.class);
                logger.error("Failed to create user {} in Keycloak. Status: {}, Reason: {}",
                        userRequest.getEmail(), response.getStatus(), errorMessage);
                throw new RuntimeException("Keycloak user creation failed: " + response.getStatusInfo().getReasonPhrase() + " - " + errorMessage);
            }
        } catch (Exception e) {
            logger.error("Exception during Keycloak user creation for {}: {}", userRequest.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Keycloak user creation failed due to an exception.", e);
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }
}