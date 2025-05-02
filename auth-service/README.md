# Auth Service

Authentication service for the Library Management System. This service handles user authentication and JWT token management.

## Features

- JWT-based authentication
- Integration with User Service for credential validation
- Role-based access control
- Stateless authentication
- Secure password handling

## Architecture

```
auth-service/
├── src/
│   └── main/
│       ├── java/com/librarysystem/authservice/
│       │   ├── config/                  # Configuration classes
│       │   │   └── SecurityConfig.java  # Spring Security setup
│       │   ├── controller/              # REST endpoints
│       │   │   └── AuthController.java  # Authentication endpoints
│       │   ├── dto/                     # Data Transfer Objects
│       │   │   ├── LoginRequest.java    # Login credentials
│       │   │   └── JwtResponse.java     # JWT response wrapper
│       │   ├── security/                # Security components
│       │   │   ├── JwtAuthFilter.java   # JWT authentication filter
│       │   │   └── JwtUtils.java        # JWT helper methods
│       │   └── AuthServiceApplication.java
│       └── resources/
│           └── application.yml          # Service configuration
├── Dockerfile                           # Container definition
└── pom.xml                             # Dependencies
```

## Technical Stack

- Java 21
- Spring Boot 3.4.5
- Spring Security
- Spring Cloud (Eureka Client, OpenFeign)
- JWT (JSON Web Token)
- PostgreSQL
- Docker

## Configuration

### Application YML file

```yaml
server:
  port: 8084

spring:
  application:
    name: auth-service
  datasource:
    url: jdbc:postgresql://postgres:5432/library?currentSchema=auth
    username: auth_service
    password: authpass

app:
  jwt:
    secret: ${JWT_SECRET:your-512-bit-secret-key}
    expiration-ms: ${JWT_EXPIRATION:86400000}  # 24h
```

### Environment Variables

- `JWT_SECRET`: Secret key for signing JWT tokens (min 64 bytes for HS512)
- `JWT_EXPIRATION`: Token expiration time in milliseconds

## API Endpoints

### Authentication

```http
POST /api/auth/login
Content-Type: application/json

{
    "email": "user@example.com",
    "password": "password123"
}
```

Response:
```json
{
    "token": "eyJhbGciOiJIUzUxMiJ9..."
}
```

## Security

- Uses HMAC-SHA512 for token signing
- Implements stateless authentication
- Token validation on each request
- Integration with service discovery for secure service communication

## Dependencies

- Discovery Service (Eureka) for service registration
- User Service for credential validation
- PostgreSQL database

## Testing

Run the tests:
```bash
mvn test
```

Integration tests use TestContainers for PostgreSQL:
```bash
mvn verify
```

## Monitoring

Health check endpoint:
```http
GET /actuator/health
```

## Notes

- Ensure proper secret key length for production (minimum 512 bits for HS512)
- Configure appropriate token expiration for your use case
- Review security settings before production deployment