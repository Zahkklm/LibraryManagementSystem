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