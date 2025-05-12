````
user-service/
├── src/
│   ├── main/
│   │   ├── java/com/librarysystem/userservice/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   └── UserServiceApplication.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/librarysystem/userservice/
├── Dockerfile
└── pom.xml
````

## Recommended User Registration and Login Flow

This document outlines a recommended flow for user registration and login, leveraging Keycloak as the central identity provider.

### Recommended User Registration Flow

1.  **Client Request:** The client application initiates user registration by making a POST request to the `/api/users/register` endpoint (or simply `/api/users` with the POST method). This request includes the necessary registration data, such as email, password, and potentially other user details.

2.  **user-service - Email Existence Check:** The `user-service` receives the registration request and first checks its own database to determine if the provided email address already exists.

3.  **user-service - Keycloak User Creation:** If the email is not found in the `user-service` database, the service then calls the Keycloak Admin REST API to create a new user within Keycloak. This call includes the user's email and password.

4.  **user-service - Keycloak UUID Retrieval:** Upon successful user creation in Keycloak, the Keycloak Admin REST API returns a unique identifier (UUID), often referred to as the `sub` (subject) claim in JWTs, for the newly created user.

5.  **user-service - User Data Persistence:** The `user-service` then saves the new user's information into its own database. Crucially, it uses the Keycloak-provided UUID as the primary key (`id`) for the user record in its local database. This ensures a consistent and globally unique identifier across systems.

6.  **user-service - Success Response:** Finally, the `user-service` returns a success response to the client application, indicating that the user registration was successful.

```mermaid
sequenceDiagram
    participant Client
    participant "user-service" as UserService
    participant Keycloak
    database UserServiceDB
    database KeycloakDB

    Client->>UserService: POST /api/users/register (email, password, ...)
    UserService->>UserServiceDB: Check if email exists
    alt Email does not exist
        UserService->>Keycloak: Call Keycloak Admin REST API to create user (email, password)
        Keycloak-->>UserService: Returns Keycloak UUID (sub)
        UserService->>UserServiceDB: Save user data (id=Keycloak UUID, email, ...)
        UserService-->>Client: Success
    else Email exists
        UserService-->>Client: Error: Email already registered
    end
```