package com.librarysystem.bookservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

/**
 * Data Transfer Object (DTO) for creating new books in the library system.
 * <p>
 * This class represents the request payload when adding a new book through the
 * {@code POST /api/books} endpoint. It includes validation constraints to ensure
 * data integrity before a book is added to the database.
 * <p>
 * The validation ensures that required fields are present and properly formatted,
 * with specialized validation for the ISBN to ensure it follows standard formats.
 * When the {@code availableCopies} field is not provided, it defaults to the value
 * of {@code totalCopies} in the service layer.
 */
@Data
public class CreateBookRequest {

    /**
     * The title of the book.
     * <p>
     * Must not be blank and cannot exceed 255 characters in length.
     */
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    /**
     * The author(s) of the book.
     * <p>
     * Must not be blank and cannot exceed 255 characters in length.
     * For multiple authors, they should be concatenated with appropriate separators.
     */
    @NotBlank(message = "Author is required")
    @Size(max = 255, message = "Author name cannot exceed 255 characters")
    private String author;

    /**
     * The International Standard Book Number (ISBN) of the book.
     * <p>
     * Must not be blank and must match a valid ISBN-10 or ISBN-13 format.
     * The regex pattern validates common ISBN formats including those with hyphens or spaces.
     */
    @NotBlank(message = "ISBN is required")
    @Pattern(regexp = "^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$", message = "Invalid ISBN format")
    private String isbn;

    /**
     * The publisher of the book.
     * <p>
     * Optional field that cannot exceed 255 characters if provided.
     */
    @Size(max = 255, message = "Publisher cannot exceed 255 characters")
    private String publisher;

    /**
     * The date when the book was published.
     * <p>
     * Must be in the past or present, cannot be a future date.
     */
    @PastOrPresent(message = "Publication date must be in the past or present")
    private LocalDate publicationDate;

    /**
     * The total number of copies of this book in the library's inventory.
     * <p>
     * Must not be null and cannot be negative.
     */
    @NotNull(message = "Total copies are required")
    @Min(value = 0, message = "Total copies cannot be negative")
    private Integer totalCopies;

    /**
     * The number of copies currently available for borrowing.
     * <p>
     * Optional field that cannot be negative if provided.
     * If not provided, it defaults to the same value as totalCopies in the service layer,
     * indicating that all copies are initially available.
     */
    @Min(value = 0, message = "Available copies cannot be negative")
    private Integer availableCopies;
}