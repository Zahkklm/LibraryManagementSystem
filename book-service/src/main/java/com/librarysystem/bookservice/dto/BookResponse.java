package com.librarysystem.bookservice.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

/**
 * Data Transfer Object (DTO) representing a book in response payloads.
 * <p>
 * This class serves as the standardized response format for all book-related
 * API endpoints, providing a consistent structure for book data returned to clients.
 * It is used by the BookController for GET, POST, and PUT operations.
 * <p>
 * The class uses Lombok's {@code @Builder} to facilitate easy object creation
 * in the service layer when mapping from domain entities to DTOs.
 */
@Data
@Builder
public class BookResponse {
    /**
     * Unique identifier for the book in the database.
     */
    private Long id;
    
    /**
     * The title of the book.
     */
    private String title;
    
    /**
     * The author(s) of the book.
     * For multiple authors, this is typically a concatenated string.
     */
    private String author;
    
    /**
     * International Standard Book Number (ISBN).
     * Globally unique identifier for the book following ISBN-10 or ISBN-13 format.
     */
    private String isbn;
    
    /**
     * The publisher of the book.
     */
    private String publisher;
    
    /**
     * The date when the book was published.
     */
    private LocalDate publicationDate;
    
    /**
     * Number of copies currently available for borrowing.
     * This count decreases when books are borrowed and increases when they're returned.
     */
    private Integer availableCopies;
    
    /**
     * Total number of copies owned by the library.
     * This represents the library's total inventory of this title.
     */
    private Integer totalCopies;
}