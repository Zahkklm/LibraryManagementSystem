# Book Service - Library Management System

## Overview

Book Service is a core microservice component of the Library Management System, responsible for managing the library's book collection. It provides a comprehensive API for creating, retrieving, updating, and deleting books, with proper inventory tracking and validation.

## Features

- Complete CRUD operations for books with role-based access control
- ISBN validation and uniqueness enforcement
- Book search by ID and ISBN (with extensibility for title and author search)
- Inventory tracking with available and total copies management
- Automatic inventory constraint validation via JPA lifecycle hooks
- API documentation with OpenAPI/Swagger

## Architecture

The Book Service follows a layered architecture pattern:

```
book-service/
├── controller/         # REST API endpoints (BookController)
├── service/            # Business logic (BookService)
├── repository/         # Data access (BookRepository)
├── entity/             # Domain model (Book)
├── dto/                # Data Transfer Objects
│   ├── BookResponse    # Response DTOs
│   ├── CreateBookRequest # Create operation DTOs 
│   └── UpdateBookRequest # Update operation DTOs
├── exception/          # Exception handling
└── config/             # Security configuration
```

## API Endpoints

| Endpoint                 | Method | Description                     | Required Role            |
|--------------------------|--------|---------------------------------|--------------------------|
| `/api/books`             | GET    | List all books                  | MEMBER, LIBRARIAN, ADMIN |
| `/api/books/{id}`        | GET    | Get book by ID                  | MEMBER, LIBRARIAN, ADMIN |
| `/api/books/isbn/{isbn}` | GET    | Get book by ISBN                | MEMBER, LIBRARIAN, ADMIN |
| `/api/books`             | POST   | Add a new book                  | LIBRARIAN, ADMIN         |
| `/api/books/{id}`        | PUT    | Update an existing book         | LIBRARIAN, ADMIN         |
| `/api/books/{id}`        | DELETE | Remove a book                   | LIBRARIAN, ADMIN         |

## Security

The service implements a two-layer security approach:

1. **Gateway Validation**: Ensures requests originate from the trusted API Gateway via the `X-Gateway-Secret` header
2. **Role-Based Access Control**: Uses Spring Security with `@PreAuthorize` annotations
   - Read operations: Available to MEMBER, LIBRARIAN, ADMIN roles
   - Write operations: Restricted to LIBRARIAN, ADMIN roles

## Configuration

Key configuration properties in `application.yml`:

```yaml
spring:
  application:
    name: book-service
  datasource:
    url: jdbc:postgresql://postgres:5432/library?currentSchema=books
    username: ${DB_USERNAME:book_service}
    password: ${DB_PASSWORD:bookpass}

eureka:
  client:
    serviceUrl:
      defaultZone: ${EUREKA_URL:http://discovery-service:8761/eureka}

api:
  gateway:
    secret: ${API_GATEWAY_SECRET:gateway-secret}
```

## Database Schema 

The service uses the books schema in the PostgreSQL database with the following structure:

Table: books
Columns:
  - id (BIGINT, PK, AUTO INCREMENT)
  - title (VARCHAR(255), NOT NULL)
  - author (VARCHAR(255), NOT NULL)
  - isbn (VARCHAR(255), NOT NULL, UNIQUE)
  - publisher (VARCHAR(255))
  - publication_date (DATE)
  - total_copies (INTEGER, NOT NULL)
  - available_copies (INTEGER, NOT NULL)


## Dependencies

- Spring Boot 3.x
- Spring Data JPA
- Spring Cloud (Eureka Client)
- Spring Security
- PostgreSQL Driver
- Springdoc OpenAPI
- Lombok

Related Services
- API Gateway: Routes and secures requests to Book Service
- Discovery Service: Provides service registration and discovery
- Auth Service: Handles authentication and provides user details
- User Service: Manages user accounts and permissions