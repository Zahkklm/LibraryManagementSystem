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
 * - status: Status of the borrow request (e.g., PENDING, RESERVED, FAILED).
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Borrow {
    @Id
    private String id;
    private Long userId;
    private Long bookId;
    private String status; // PENDING, RESERVED, FAILED, CANCELLED
    private LocalDate returnDate;
}