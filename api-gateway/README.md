# API Gateway Service

The API Gateway service is the single entry point for all client requests to the Library Management System's microservices. It handles routing, security (token validation, role extraction), rate limiting (if implemented), and other cross-cutting concerns.

## Features

- **Centralized Request Routing:** Routes incoming requests to the appropriate downstream microservices (e.g., User Service, Book Service, Auth Service).
- **Security Enforcement:**
    - Validates JWT Bearer tokens received from clients.
    - Extracts user roles from the token.
    - Forwards user roles (e.g., via `X-User-Roles` header) to downstream services for fine-grained authorization.
- **Load Balancing:** Leverages Spring Cloud LoadBalancer (integrated with Eureka) to distribute requests among instances of downstream services.
- **Path-Based Routing:** Configures routes based on URL paths to direct traffic.
- **Service Discovery Integration:** Uses Eureka to dynamically discover downstream service instances.

## Architecture

The API Gateway sits between external clients (e.g., web/mobile applications) and the internal microservices. It acts as a reverse proxy and a security checkpoint.

```
Client ---> API Gateway ---(Authenticated & Routed Request)---> Downstream Microservice (e.g., User Service)
                  |
                  +--- Validates Token with Keycloak (or relies on token introspection if configured)
                  |
                  +--- Discovers services via Eureka
```

Refer to the main project [README.md](../../README.md) for the overall system architecture diagram.

## Technical Stack

- Java 21
- Spring Boot 3.x.x (Update with your specific version)
- Spring Cloud Gateway
- Spring Security (for token validation and processing)
- Spring Cloud Netflix Eureka Client (for service discovery)
- Docker

## Configuration

Key configurations are managed in `src/main/resources/application.yml` and can be overridden by environment variables in `docker-compose.yml`.

### `application.yml` Highlights:

```yaml
server:
  port: 8080 # Default port for the API Gateway

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true # Enables route discovery via Eureka
          lower-case-service-id: true # Converts service IDs to lowercase for routing
      routes:
        # Example explicit route (you might have more or rely on discovery locator)
        - id: auth_service_route
          uri: lb://auth-service # lb:// for load balancing via Eureka
          predicates:
            - Path=/api/auth/**
          filters:
            - StripPrefix=2 # Example: /api/auth/login -> /login on auth-service

# Eureka Client Configuration
eureka:
  client:
    serviceUrl:
      defaultZone: ${EUREKA_URL:http://discovery-service:8761/eureka}
  instance:
    preferIpAddress: true

# Keycloak Configuration (for token validation and role extraction)
keycloak:
  realm: ${KEYCLOAK_REALM:library}
  auth-server-url: ${KEYCLOAK_URL:http://keycloak:8080} # Base URL of Keycloak
  resource: ${KEYCLOAK_CLIENT_ID:library-client} # Client ID used by the gateway if it needs to introspect tokens
  # public-client: true # Or credentials if using a confidential client for introspection

# Logging
logging:
  level:
    org.springframework.cloud.gateway: INFO
    # com.librarysystem.apigateway: DEBUG # For custom filters/logic
```

### Environment Variables (in `docker-compose.yml`):

- `SPRING_PROFILES_ACTIVE=docker`
- `KEYCLOAK_URL`: URL of the Keycloak server (e.g., `http://keycloak:8080`)
- `EUREKA_URL`: URL of the Eureka discovery service (e.g., `http://discovery-service:8761/eureka`)
- `KEYCLOAK_REALM` (Optional, if different from default in `application.yml`)
- `KEYCLOAK_CLIENT_ID` (Optional, if different from default in `application.yml`)

## Security

- **JWT Validation:** The gateway is configured with Spring Security to validate JWTs. It typically uses the public key from Keycloak (via JWKS URI) to verify token signatures.
- **Role Propagation:** After validating a token, the gateway extracts user roles (e.g., from the `realm_access.roles` claim) and adds them to a custom header (e.g., `X-User-Roles`) for downstream services. This is handled by custom filters like `AddRolesHeaderGatewayFilterFactory.java`.
- **Path Protection:** While downstream services perform their own authorization, the API Gateway can also be configured with security rules to protect certain paths based on roles, further securing the system at the entry point.

## Dependencies

- **Discovery Service (Eureka):** For locating downstream microservices.
- **Keycloak:** For JWT validation (obtaining public keys/JWKS URI).
- **Downstream Microservices:** (e.g., User Service, Book Service, Auth Service) to which it routes requests.

## Running and Testing

The API Gateway is started as part of the `docker-compose up` command defined in the main project.

- **Access Point:** `http://localhost:8080`

To test:
1. Obtain a JWT token by logging in via the `/api/auth/login` endpoint (which routes to the Auth Service).
2. Make requests to other service endpoints (e.g., `/api/users/1`, `/api/books`) through the API Gateway, including the obtained JWT in the `Authorization: Bearer <token>` header.

Example:
```bash
# Login (routed to Auth Service)
curl -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{"email": "admin@example.com", "password": "admin123"}'

# Assuming the above returns a token, then:
# Get user (routed to User Service, with token)
curl -X GET http://localhost:8080/api/users/1 \
-H "Authorization: Bearer <your_jwt_token_here>"
```

Check the logs of the API Gateway and downstream services to observe routing and security processing.
`docker-compose logs api-gateway`

## Project Structure:

```
api-gateway/
├── src/main/java/com/librarysystem/apigateway/
│   ├── ApiGatewayApplication.java
│   ├── config/
│   │   └── SecurityConfig.java
│   ├── filter/
│   │   └── LoggingFilter.java
│   └── security/annotation/
│       ├── AdminOnly.java
│       ├── LibrarianOrAdmin.java
│       └── MemberAccess.java
└── src/main/resources/
    └── application.yml
```