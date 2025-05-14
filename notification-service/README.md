# Reactive Notification Service

A reactive microservice for handling real-time notifications in the Library Management System.

## Overview

The Notification Service is a reactive Spring-based microservice that provides real-time notifications to users of the Library Management System. It leverages Spring WebFlux, R2DBC, and Server-Sent Events (SSE) to deliver notifications to clients with minimal latency.

## Architecture

This service follows reactive programming principles, enabling non-blocking, event-driven notification delivery:

- **Event-Driven**: Listens to Kafka events from other services (book-service, borrow-service)
- **Reactive Stack**: Uses Spring WebFlux and R2DBC for full reactive processing
- **Real-Time Delivery**: Implements Server-Sent Events for pushing notifications to clients
- **Per-User Streams**: Maintains separate notification streams for each connected user
- **Persistence**: Stores notifications in PostgreSQL using reactive repositories
- **SAGA Observer**: Acts as an observer to the choreography-based SAGA pattern between book-service and borrow-service

## Features

- Real-time notification delivery via SSE
- Automatic conversion of Kafka events to user notifications
- Persistent storage of all notifications
- REST API for retrieving user notifications
- Support for various notification types (book reserved, reservation failed, book returned, book overdue)
- No additional service calls - works entirely with enriched events

## Technologies

- **Spring Boot 3.x**: Core framework
- **Spring WebFlux**: Reactive web framework
- **Spring Data R2DBC**: Reactive database access
- **Project Reactor**: Reactive streams implementation
- **Kafka**: Event streaming platform
- **PostgreSQL**: Persistent storage
- **Docker**: Containerization

## API Endpoints

| Endpoint | Method | Description | Response |
|----------|--------|-------------|----------|
| `/api/notifications/stream/{userId}` | GET | Stream real-time notifications (SSE) | `Flux<NotificationEvent>` |
| `/api/notifications/{userId}` | GET | Retrieve all notifications for a user | `Flux<Notification>` |

## Kafka Event Topics

| Topic | Purpose | Data |
|-------|---------|------|
| `book-reserved` | When a book is successfully reserved | borrowId, bookId, userId, bookTitle, author, isbn |
| `book-reserve-failed` | When a book reservation fails | borrowId, userId, bookId, bookTitle, reason |
| `borrow-confirmed` | When a borrow is confirmed | borrowId, bookId, userId, bookTitle, author, isbn, dueDate |
| `borrow-failed` | When a borrow fails | borrowId, bookId, userId, bookTitle, reason |
| `book-return-confirmed` | When a book return is confirmed | borrowId, bookId, userId, bookTitle, author, isbn |
| `borrow-overdue` | When a borrowed book becomes overdue | borrowId, bookId, userId, bookTitle, dueDate |

## Integration with SAGA Pattern

The Notification Service acts as an observer to the choreography-based SAGA pattern implemented between the book-service and borrow-service:

1. It listens to event outcomes from the SAGA transactions
2. It creates user-friendly notifications based on these events
3. It does not participate in or affect the transaction flow
4. It relies entirely on the enriched event data without making service-to-service calls

## Setup Instructions

### Prerequisites
- Java 21+
- Maven 3.6+
- Docker & Docker Compose
- PostgreSQL 15+
- Kafka