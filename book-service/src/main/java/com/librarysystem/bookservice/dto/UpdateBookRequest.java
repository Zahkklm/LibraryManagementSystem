package com.librarysystem.bookservice.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

/**
 * Data Transfer Object (DTO) for updating existing books in the library system.
 * <p>
 * This class represents the request payload when modifying a book through the
 * {@code PUT /api/books/{id}} endpoint. Unlike the CreateBookRequest, all fields
 * are optional, allowing partial updates to book information. Only the fields
 * that are provided will be updated in the existing book record.
 * <p>
 * While fields are optional, when provided they must still pass validation
 * constraints to ensure data integrity. The service layer handles null checks
 * and only updates fields that are explicitly provided in the request.
 * <p>
 * This approach follows the PATCH-like semantics while still using the PUT verb,
 * which is a common pattern in REST APIs for simplifying partial updates.
 */
@Data
public class UpdateBookRequest {

    /**
     * The updated title of the book.
     * <p>
     * Optional field that cannot exceed 255 characters if provided.
     * If not provided (null), the existing title will be retained.
     */
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    /**
     * The updated author(s) of the book.
     * <p>
     * Optional field that cannot exceed 255 characters if provided.
     * If not provided (null), the existing author information will be retained.
     */
    @Size(max = 255, message = "Author cannot exceed 255 characters")
    private String author;

    /**
     * The updated ISBN (International Standard Book Number) of the book.
     * <p>
     * Optional field that must be between 10 and 13 characters if provided.
     * If not provided (null), the existing ISBN will be retained.
     * Note: Updating ISBN should be handled carefully as it might create duplicates.
     * The service layer should perform additional validation beyond these constraints.
     */
    @Size(min = 10, max = 13, message = "ISBN must be between 10 and 13 characters")
    private String isbn;

    /**
     * The updated publisher of the book.
     * <p>
     * Optional field that cannot exceed 255 characters if provided.
     * If not provided (null), the existing publisher information will be retained.
     */
    @Size(max = 255, message = "Publisher cannot exceed 255 characters")
    private String publisher;

    /**
     * The updated publication date of the book.
     * <p>
     * Optional field with no explicit validation constraints.
     * If not provided (null), the existing publication date will be retained.
     * The service layer should enforce appropriate business rules (e.g., not future dates).
     */
    private LocalDate publicationDate;

    /**
     * The updated total number of copies of this book in the library's inventory.
     * <p>
     * Optional field that must be zero or positive if provided.
     * If not provided (null), the existing total copies value will be retained.
     * The service layer should enforce appropriate business rules (e.g., cannot be less
     * than the number of currently borrowed copies).
     */
    @PositiveOrZero(message = "Total copies must be zero or positive")
    private Integer totalCopies;

    /**
     * The updated number of copies currently available for borrowing.
     * <p>
     * Optional field that must be zero or positive if provided.
     * If not provided (null), the existing available copies value will be retained.
     * The service layer should enforce appropriate business rules (e.g., cannot exceed
     * total copies).
     */
    @PositiveOrZero(message = "Available copies must be zero or positive")
    private Integer availableCopies;
}