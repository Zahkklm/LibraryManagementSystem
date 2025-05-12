package com.librarysystem.borrowservice.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a borrow request in the library system.
 * 
 * Fields:
 * - id: Unique identifier for the borrow request.
 * - userId: ID of the user making the borrow request.
 * - bookId: ID of the book being borrowed.
 * - status: Status of the borrow request (e.g., PENDING, RESERVED, FAILED, CANCELLED, RETURNED).
 * - returnDate: The date the book was returned.
 * - dueDate: The date the book is due to be returned.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Borrow {
    @Id
    private String id;           // Unique identifier for the borrow request
    private Long userId;         // ID of the user making the borrow request
    private Long bookId;         // ID of the book being borrowed
    private String status;       // Borrow status: PENDING, RESERVED, FAILED, CANCELLED, RETURNED
    private LocalDate returnDate; // Date the book was returned
    private LocalDate dueDate;    // Date the book is due to be returned
}