# Library Management System

A feature-rich and scalable Library Management System built with a microservices architecture. This system enables librarians to manage books, users, borrows, and more, designed to function effectively under a significant load. It leverages modern Java technologies, Spring Boot, Spring Cloud, Keycloak for identity management, and Docker for containerization.

## Architecture Overview

The system employs a microservice architecture where distinct services handle specific business domains. These services are designed to be independently deployable and scalable. Communication is managed through an API Gateway, with service discovery facilitated by Netflix Eureka. Keycloak serves as the centralized Identity and Access Management (IAM) solution.

## Service Decomposition

| Service                | Responsibility                                                                 | Tech Stack                                      | Port | Database Schema | API Gateway Routing |
|------------------------|--------------------------------------------------------------------------------|-------------------------------------------------|------|-----------------|---------------------|
| **Discovery Service**  | Service Registration and Discovery                                             | Netflix Eureka                                  | 8761 | -               | -                   |
| **API Gateway**        | Request Routing, JWT Validation, Header Enrichment, CORS, Central Entry Point  | Spring Cloud Gateway, Spring Security (OAuth2 RS) | 8080 | -               | * (all requests)    |
| **Auth Service**       | User Authentication Orchestration, JWT Issuance (via Keycloak ROPC grant)      | Spring Boot, Spring Security, Feign             | 8084 | auth (minimal)  | /api/auth/**        |
| **User Service**       | User CRUD, Role Management, Credential Validation, Keycloak User Synchronization | Spring Boot, Spring MVC, JPA, Keycloak Admin API  | 8082 | users           | /api/users/**       |
| **Book Service**       | (Planned) Book CRUD, Search, Inventory Management                              | Spring Boot (e.g., WebFlux, R2DBC or JPA)       | 8081 | books           | /api/books/**       |
| **Borrow Service**     | (Planned) Borrow/Return Logic, Overdue Tracking, Fine Calculation              | Spring Boot, Spring MVC, JPA                    | 8083 | borrows         | /api/borrows/**     |
| **Saga Service**       | (Planned) Distributed Transaction Orchestration (e.g., for complex borrows)    | Spring Boot, Kafka (or other messaging)         | 8085 | saga            | /api/saga/**        |
| **Notification Service** | (Planned) User Notifications (e.g., Overdue Alerts, New Book Announcements)    | Spring Boot, Kafka, WebSocket (optional)        | 8086 | -               | /api/notifications/** |

## Key Architectural Concepts & Features

*   **Microservice Architecture:** Promotes modularity, independent scaling, and technology diversity.
*   **Centralized Authentication & Authorization (Keycloak):**
    *   Keycloak manages user identities, roles, and issues JWTs.
    *   `auth-service` orchestrates authentication, using Keycloak's ROPC grant for token issuance.
*   **API Gateway (Spring Cloud Gateway):**
    *   Single entry point for all client requests.
    *   Acts as an OAuth2 Resource Server, validating JWTs.
    *   Enriches requests to downstream services with user-specific headers (`X-User-Id`, `X-User-Roles`, `X-User-Email`) via `AddUserHeadersGatewayFilterFactory`.
    *   Secures inter-service communication by adding a shared secret header (`X-Gateway-Secret`).
*   **Service Discovery (Netflix Eureka):** Enables dynamic lookup and routing of services.
*   **User Synchronization with Keycloak:**
    *   `user-service` uses the Keycloak Admin API to create users in Keycloak when they are registered locally. This ensures consistency and allows users to authenticate via Keycloak immediately after registration. This process is transactional.
*   **Inter-Service Security:**
    *   Downstream services (`auth-service`, `user-service`) validate the `X-Gateway-Secret` header to ensure requests originate from the API Gateway.
*   **Role-Based Access Control (RBAC):**
    *   `user-service` (and other future services) use a `RoleExtractionFilter` to establish the security context based on `X-User-Roles` propagated by the API Gateway.
    *   Fine-grained access control is applied using Spring Security's `authorizeHttpRequests` and method-level security (`@PreAuthorize` with custom annotations like `@AdminOnly`).
*   **Schema-per-Service:** PostgreSQL schemas provide data isolation for each service using the same database instance.
*   **Containerization (Docker & Docker Compose):** Simplifies deployment, development setup, and ensures consistency across environments.
*   **Configuration Management:** Centralized in `application.yml` files per service, with support for environment variable overrides.

## Security Flow Overview

1.  **Login:**
    *   Client sends credentials to `POST /api/auth/login` via the API Gateway.
    *   API Gateway routes the request to `auth-service`.
    *   `auth-service` validates the `X-Gateway-Secret` (added by API Gateway for this internal hop if configured, though login is often public at gateway).
    *   `auth-service` calls `user-service` (`/api/users/validate`) to verify credentials against the local user database.
    *   If valid, `auth-service` requests a JWT from Keycloak using the ROPC grant.
    *   Keycloak issues a JWT, which `auth-service` returns to the client via the API Gateway.
2.  **Accessing Protected Resources:**
    *   Client includes the JWT in the `Authorization` header for requests to protected endpoints (e.g., `GET /api/users/{id}`).
    *   API Gateway intercepts the request:
        *   Validates the JWT against Keycloak's public keys.
        *   If valid, extracts user ID, email, and roles.
        *   Adds `X-User-Id`, `X-User-Email`, `X-User-Roles`, and `X-Gateway-Secret` headers.
        *   Routes the enriched request to the appropriate downstream service (e.g., `user-service`).
    *   Downstream Service (`user-service`):
        *   `GatewayValidationFilter` validates the `X-Gateway-Secret`.
        *   `RoleExtractionFilter` processes `X-User-Roles` to establish the `SecurityContext`.
        *   Spring Security authorizes the request based on configured rules (e.g., `hasRole('MEMBER')`).

## Technology Stack

*   **Backend:** Java 21, Spring Boot 3.x, Spring Cloud (Gateway, Netflix Eureka)
*   **Identity & Access Management:** Keycloak (version 24.0)
*   **Database:** PostgreSQL
*   **Inter-service Communication:** REST (Spring MVC, Feign Clients)
*   **Build Tool:** Apache Maven
*   **Containerization:** Docker, Docker Compose

## Prerequisites

*   Java JDK 21 or later
*   Apache Maven 3.6+
*   Docker & Docker Compose

**Minimum System Requirements (for local development with all services):**
*   CPU: 4 cores (6+ recommended)
*   RAM: 8GB (16GB recommended for smoother experience, especially with multiple services and Keycloak)
*   Disk Space: 10GB (ensure Docker has sufficient disk allocation)

## Getting Started

### Build and Run

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/Zahkklm/LibraryManagementSystem.git
    cd LibraryManagementSystem
    ```

2.  **Build all services and start containers:**
    *   Build each service individually using Maven (from the project root or within each service directory):
        ```bash
        # Example for user-service
        cd user-service
        mvn clean package -DskipTests
        cd ..
        # Repeat for other services: api-gateway, auth-service, discovery-service, etc.
        ```
    *   Then run Docker Compose from the project root to build images and start the containers:
        ```bash
        docker-compose up --build
        ```

### Access Points

*   **API Gateway:** `http://localhost:8080` (Main entry point for application APIs)
*   **Discovery Service (Eureka Dashboard):** `http://localhost:8761`
*   **Keycloak Admin Console:** `http://localhost:8090`
    *   Credentials: `admin` / `admin` (as per `docker-compose.yml` environment variables for Keycloak)
    *   Realm: `library`
*   **PgAdmin (Database Management):** `http://localhost:5050`
    *   Credentials: `admin@example.com` / `adminadmin123` (as per `docker-compose.yml`)
    *   To connect to the PostgreSQL server:
        *   Host: `postgres` (service name in Docker Compose)
        *   Port: `5432`
        *   Username: `admin`
        *   Password: `adminadmin`

## Project Structure

```
.
├── discovery-service       # Eureka service discovery
├── api-gateway             # Spring Cloud API Gateway
├── auth-service            # Authentication orchestration service
├── user-service            # User management service
├── book-service            # (Planned) Book management service
├── borrow-service          # (Planned) Borrowing management service
├── saga-service            # (Planned) Distributed transaction management
├── notification-service    # (Planned) Notification service
├── docker-compose.yml      # Docker orchestration file
├── README.md               # This file
├── LowLevelDesign.png      # (If available) Diagram of the architecture
├── keycloak/import/realm-export.json # Keycloak realm configuration for import
└── init.sql                # PostgreSQL initialization script (schema, roles)
```

## Considerations & Tradeoffs

*   **Resource Consumption:** Running multiple JVMs for each microservice, plus Keycloak and PostgreSQL, can be resource-intensive for local development.
*   **Development Complexity:** Microservices introduce challenges in inter-service communication, distributed transactions (planned via Saga), debugging, and end-to-end testing.
*   **Database Strategy:**
    *   Schema-per-service provides good data isolation within a shared PostgreSQL instance.
    *   For production, consider dedicated database instances or managed cloud database services for higher availability and independent scaling of data tiers.
*   **Inter-Service Communication:** Currently relies on synchronous REST calls (Feign). For long-running operations or to improve resilience, asynchronous communication (e.g., Kafka, RabbitMQ) should be considered, especially for planned services like `saga-service` and `notification-service`.
*   **Keycloak Admin Client in API Gateway:** The `api-gateway`'s `pom.xml` includes `keycloak-admin-client`. This dependency is typically not needed for a gateway acting solely as a resource server and router. Review if this is intentional for future administrative tasks from the gateway or if it can be removed.
*   **Dependency Version Alignment:** Minor variations in Spring Boot/Cloud versions exist across services. Standardizing these can simplify long-term maintenance.

## Future Work & Enhancements

*   **Implement Core Business Logic:**
    *   `book-service`: Full CRUD for books, search capabilities, inventory tracking.
    *   `borrow-service`: Logic for borrowing, returning, extending due dates, and calculating fines.
*   **Distributed Transactions (`saga-service`):** Implement Saga pattern for operations spanning multiple services (e.g., a complete book borrowing process).
*   **Notifications (`notification-service`):** Develop real-time or batch notifications for events like overdue books, new book arrivals, etc.
*   **Resilience Patterns:**
    *   Fully implement and configure circuit breakers (e.g., Resilience4j, already a dependency in `auth-service`) for all critical inter-service calls.
    *   Implement robust retry mechanisms with backoff strategies.
*   **Observability:**
    *   Integrate distributed logging (e.g., ELK Stack - Elasticsearch, Logstash, Kibana).
    *   Implement distributed tracing (e.g., Micrometer Tracing with Zipkin or Jaeger) for end-to-end request visibility.
    *   Set up comprehensive monitoring and alerting (e.g., Prometheus, Grafana).
*   **User Lifecycle Management:** Complete user deactivation/deletion synchronization with Keycloak in `user-service`.
*   **API Documentation:** Generate and maintain API documentation (e.g., using Springdoc OpenAPI).
*   **Testing:** Enhance with more comprehensive integration tests and contract testing between services.
*   **Security Hardening:** Regular dependency updates, security scans, and review of Keycloak configurations.