## Borrowing and Returning (Saga Pattern)

### Borrow a Book

- **Patron Action:** User sends a borrow request via `/api/borrows`.
- **Borrow Service:**
  - Checks user eligibility (e.g., not blocked, no overdue books).
  - Creates a `Borrow` record with status `PENDING`, sets borrowing and due dates.
  - Publishes a `book-reserve-requested` event to Kafka with `borrowId`, `bookId`, `userId`, and relevant dates.
- **Book Service:**
  - Listens for `book-reserve-requested` events.
  - Checks if the book is available.
    - If available: decrements available copies, publishes `book-reserved` event.
    - If not available: publishes `book-reserve-failed` event.
- **Borrow Service:**
  - Listens for `book-reserved` and `book-reserve-failed` events.
    - On `book-reserved`: updates borrow status to `RESERVED`.
    - On `book-reserve-failed`: updates borrow status to `FAILED`.

### Return a Book

- **Patron Action:** User requests to return a book.
- **Borrow Service:**
  - Updates the `Borrow` record status to `RETURNED`, records return date.
  - Publishes a `book-returned` event to Kafka with `borrowId`, `bookId`.
- **Book Service:**
  - Listens for `book-returned` events.
  - Increments available copies for the book.

### View Borrowing History

- **Users:** Can query `/api/borrows/history` for their own borrow records.
- **Librarians:** Can query all users' borrowing history.

### Manage Overdue Books

- **Borrow Service:**
  - Periodically checks for borrows where due date < today and status is not `RETURNED`.
  - Marks such borrows as `OVERDUE`.
  - Librarians can query `/api/borrows/overdue` for reports.

---

### Saga Event Flow Example

| Step                | Service         | Kafka Topic              | Event Data                          | Action                                  |
|---------------------|----------------|--------------------------|--------------------------------------|------------------------------------------|
| Borrow request      | borrow-service | book-reserve-requested   | borrowId, bookId, userId, dates     | Save borrow, publish event               |
| Reserve book        | book-service   | book-reserved / failed   | borrowId, bookId                    | Update book, publish result event        |
| Update borrow status| borrow-service | book-reserved / failed   | borrowId                            | Update borrow status (RESERVED/FAILED)   |
| Return book         | borrow-service | book-returned            | borrowId, bookId                    | Update borrow, publish event             |
| Update book copies  | book-service   | (consumes book-returned) | borrowId, bookId                    | Increment available copies               |

---

**All cross-service actions (reserve, return) are coordinated via Kafka events using the Saga pattern. Each service updates its own data and publishes events for the next step, ensuring eventual consistency and robust workflow management.**

# SAGA Workflow:

## Book Borrowing Process

This document outlines the process for borrowing and returning books in a library system using a microservices architecture and event-driven communication via Kafka.

### 1. Borrow Request Initiation

- **User Action:** A user requests to borrow a book via the `/api/borrows` endpoint.
- **Borrow Service:**
    - Receives the borrow request.
    - Checks the user's eligibility to borrow.
    - Creates a new `Borrow` record in its database with the status set to `PENDING`.
    - Publishes a `book-reserve-requested` event to a Kafka topic. This event will contain information about the book and the user.

### 2. Book Reservation

- **Book Service:**
    - Listens for `book-reserve-requested` events on the Kafka topic.
    - Upon receiving an event:
        - Checks the availability of the requested book in its inventory.
        - **If Available:**
            - Decrements the `available_copies` count for the book in its database.
            - Publishes a `book-reserved` event to Kafka, including the book and borrow identifiers.
        - **If Not Available:**
            - Publishes a `book-reserve-failed` event to Kafka, including the book and borrow identifiers and the reason for failure.

### 3. Borrow Status Update

- **Borrow Service:**
    - Listens for `book-reserved` and `book-reserve-failed` events on the Kafka topic.
    - Upon receiving a `book-reserved` event:
        - Updates the status of the corresponding `Borrow` record in its database to `RESERVED`.
    - Upon receiving a `book-reserve-failed` event:
        - Updates the status of the corresponding `Borrow` record in its database to `FAILED`.

### 4. Compensation: Borrow Cancellation

- **User/System Action:** A user or the system triggers the cancellation of a borrow request (e.g., via the `/api/borrows/{borrowId}/cancel` endpoint).
- **Borrow Service:**
    - Receives the cancellation request.
    - Sets the status of the corresponding `Borrow` record in its database to `CANCELLED`.
    - Publishes a `book-reservation-cancelled` event to Kafka, including the book identifier.
- **Book Service:**
    - Listens for `book-reservation-cancelled` events on the Kafka topic.
    - Upon receiving an event:
        - Increments the `available_copies` count for the corresponding book in its database, effectively releasing the reservation.

### 5. Return Book

- **User Action:** A user requests to return a book (e.g., via the `/api/borrows/{borrowId}/return` endpoint).
- **Borrow Service:**
    - Receives the return request.
    - Sets the status of the corresponding `Borrow` record in its database to `RETURNED`.
    - Records the return date in the `Borrow` record.
    - Publishes a `book-returned` event to Kafka, including the book identifier.
- **Book Service:**
    - Listens for `book-returned` events on the Kafka topic.
    - Upon receiving an event:
        - Increments the `available_copies` count for the corresponding book in its database.

### 6. Overdue Handling (Optional)

- **Scheduled Job (Borrow Service):**
    - A scheduled job runs periodically within the Borrow service.
    - This job queries the `Borrow` database for records with a due date in the past and a status other than `RETURNED` or `CANCELLED`.
    - For each overdue borrow:
        - Sets the status of the `Borrow` record to `OVERDUE`.
        - Optionally, publishes a `borrow-overdue` event to Kafka for notifications or reporting purposes.

### 7. Notifications (Optional)

- **Borrow Service:**
    - Upon significant status changes in the `Borrow` record (e.g., `RESERVED`, `FAILED`, `CANCELLED`, `RETURNED`, `OVERDUE`), the Borrow service can optionally publish notification events to a dedicated notification service or directly to users/librarians (e.g., via email, SMS).
    - These notification events would contain relevant information about the borrow status change.