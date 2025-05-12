# Library Management System

A scalable, microservices-based Library Management System for managing books, users, borrows, and more. Built with modern Java, Spring Boot, Spring Cloud, Keycloak for identity management, and Docker for containerization.

---

## Architecture Overview

This project is a **microservices-based Library Management System** designed for scalability, modularity, and security. Each domain (users, books, borrows, etc.) is handled by a dedicated service, enabling independent development and deployment.

- **API Gateway:**  
  - The single entry point for all client requests.
  - Handles routing, JWT validation (with Keycloak), user/role header enrichment, and adds `X-Gateway-Secret` for downstream trust.
  - Integrates with Eureka for service discovery.
  - See [api-gateway/README.md](api-gateway/README.md) for details.

- **Auth Service:**  
  - Handles `/api/auth/login` and `/api/auth/register`.
  - Orchestrates authentication: validates credentials via user-service, requests JWT from Keycloak, returns token to client.
  - Uses Feign to communicate with user-service.
  - See [auth-service/README.md](auth-service/README.md).

- **User Service:**  
  - Handles `/api/users` endpoints (CRUD, credential validation).
  - On registration, creates user in Keycloak (via admin REST API), retrieves UUID, and saves user in its own DB with Keycloak UUID as primary key.
  - Validates credentials for login.
  - See [user-service/README.md](user-service/README.md).

- **Book Service:**  
  - Manages books (`/api/books` endpoints).
  - CRUD operations, inventory management, and security via roles.
  - Listens to Kafka events for book reservation/return (Saga pattern).
  - See [book-service/README.md](book-service/README.md).

- **Borrow Service:**  
  - Handles borrowing logic (`/api/borrows` endpoints).
  - Publishes and consumes Kafka events for borrow/reserve/return workflows.
  - See [borrow-service/README.md](borrow-service/README.md).

- **Notification Service:**  
  - (Planned) Will handle user notifications via Kafka/WebSocket.

- **Keycloak:**  
  - Identity provider for authentication and authorization.
  - Issues JWTs, manages users and roles.

- **Eureka Discovery Service:**  
  - Service registry for dynamic discovery and load balancing.

- **Kafka:**  
  - Used for event-driven communication (Saga pattern) between services.

- **PostgreSQL:**  
  - Each service uses its own schema or DB for data isolation.

- **Docker Compose:**  
  - Orchestrates all services, databases, and infrastructure for local development.

---

## Service Decomposition

| Service                | Responsibility                                               | Tech Stack                                    | Port | Schema   | Gateway Route         |
|------------------------|-------------------------------------------------------------|-----------------------------------------------|------|----------|----------------------|
| Discovery Service      | Service registration/discovery (Eureka)                     | Netflix Eureka                                | 8761 | -        | -                    |
| API Gateway            | Routing, JWT validation, header enrichment, CORS            | Spring Cloud Gateway, Spring Security         | 8080 | -        | * (all requests)     |
| Auth Service           | Authentication, JWT issuance (Keycloak ROPC grant)          | Spring Boot, Spring Security, Feign           | 8084 | auth     | /api/auth/**         |
| User Service           | User CRUD, roles, credential validation, Keycloak sync      | Spring Boot, Spring MVC, JPA, Keycloak Admin  | 8082 | users    | /api/users/**        |
| Book Service           | Book CRUD, search, inventory                                | Spring Boot (MVC/JPA), Spring Security        | 8081 | books    | /api/books/**        |
| Borrow Service         | Borrow/return logic, fines                                  | Spring Boot, Spring MVC, JPA                  | 8083 | borrows  | /api/borrows/**      |
| Notification Service   | User notifications (Planned)                                | Spring Boot, Kafka, WebSocket                 | 8086 | -        | /api/notifications/**|

---

## Key Features

- **Centralized Authentication & Authorization:** Keycloak manages users/roles and issues JWTs. Auth Service orchestrates login and token issuance.
- **API Gateway Security:** Validates JWTs, enriches requests with user headers (`X-User-Id`, `X-User-Email`, `X-User-Roles`), and adds `X-Gateway-Secret` for inter-service trust.
- **Role-Based Access Control:** Fine-grained access via Spring Security and custom annotations (e.g., `@AdminOnly`).
- **Schema-per-Service:** Each service uses its own PostgreSQL schema for data isolation.
- **Service Discovery:** Eureka enables dynamic routing and scaling.
- **Containerized Development:** All services, Keycloak, and PostgreSQL run in Docker containers for easy setup.

---

## Security Flow

1. **Login:**
   - Client sends credentials to `POST /api/auth/login` via API Gateway.
   - Gateway routes to Auth Service.
   - Auth Service validates `X-Gateway-Secret` (if required), then calls User Service to validate credentials.
   - If valid, Auth Service requests JWT from Keycloak (ROPC grant) and returns it to the client.

2. **Accessing Protected Resources:**
   - Client includes JWT in `Authorization` header.
   - API Gateway validates JWT, extracts user info, adds user headers and `X-Gateway-Secret`, and routes to downstream service.
   - Downstream service validates `X-Gateway-Secret`, extracts roles, and authorizes via Spring Security.

---

## Technology Stack

- **Backend:** Java 21, Spring Boot 3.x, Spring Cloud (Gateway, Eureka)
- **Identity:** Keycloak 24.0
- **Database:** PostgreSQL (schema-per-service)
- **Inter-service:** REST (Spring MVC, Feign)
- **Build:** Maven
- **Containerization:** Docker, Docker Compose

---

## Prerequisites

- Java 21+
- Maven 3.6+
- Docker & Docker Compose

**Recommended for local development:**
- CPU: 4+ cores (6+ preferred)
- RAM: 8GB+ (16GB recommended)
- Disk: 10GB+ (ensure Docker has enough space)

---

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/Zahkklm/LibraryManagementSystem.git
cd LibraryManagementSystem
```

### 2. Build & Start All Services

```bash
docker-compose up --build
```

### 3. Access Points

- **API Gateway:** [http://localhost:8080](http://localhost:8080)
- **Eureka Dashboard:** [http://localhost:8761](http://localhost:8761)
- **Keycloak Admin:** [http://localhost:8090](http://localhost:8090)
  - Credentials: `admin` / `admin`
  - Realm: `library`
- **PgAdmin:** [http://localhost:5050](http://localhost:5050)
  - Credentials: `admin@example.com` / `adminadmin123`
  - DB Host: `postgres`, Port: `5432`, User: `admin`, Pass: `adminadmin`

---

## Project Structure

```
.
├── discovery-service       # Eureka service discovery
├── api-gateway            # Spring Cloud API Gateway
├── auth-service           # Authentication orchestration
├── user-service           # User management
├── book-service           # Book management
├── borrow-service         # Borrowing management
├── notification-service   # (Planned) Notifications
├── docker-compose.yml     # Docker orchestration
├── README.md              # This file
├── LowLevelDesign.png     # Architecture diagram
├── keycloak/import/realm-export.json # Keycloak realm config
└── init.sql               # PostgreSQL schema/init script
```

---

## Considerations & Tradeoffs

- **Resource Usage:** Multiple JVMs, Keycloak, and PostgreSQL require significant resources for local dev.
- **Complexity:** Microservices add complexity in communication, transactions, and debugging.
- **Database:** Schema-per-service is used for isolation; consider dedicated DBs for production.
- **Inter-service Communication:** Synchronous REST (Feign) for now; consider async (Kafka, RabbitMQ) for resilience.
- **Gateway Dependencies:** Review if `keycloak-admin-client` is needed in API Gateway.
- **Version Alignment:** Standardize Spring Boot/Cloud versions across services.

---

## Future Enhancements

- Expand borrowing logic and fines in `borrow-service`.
- Introduce distributed transactions (`saga-service`).
- Add notifications (`notification-service`).
- Implement circuit breakers, retries, and distributed tracing/logging.
- Complete user lifecycle sync with Keycloak.
- Generate API docs (Springdoc OpenAPI).
- Expand integration and contract testing.
- Regularly update dependencies and review security.

---

**For more details on each service, see:**
- [api-gateway/README.md](api-gateway/README.md)
- [auth-service/README.md](auth-service/README.md)
- [user-service/README.md](user-service/README.md)
- [book-service/README.md](book-service/README.md)
- [borrow-service/README.md](borrow-service/README.md)