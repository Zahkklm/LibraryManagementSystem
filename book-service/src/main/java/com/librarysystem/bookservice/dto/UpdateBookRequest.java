package com.librarysystem.bookservice.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

/**
 * DTO for updating an existing book in the library.
 * <p>
 * All fields are optional; only provided fields will be updated.
 * Includes validation annotations for each field.
 * </p>
 */
@Data
public class UpdateBookRequest {

    /**
     * Title of the book.<br>
     * Optional. If provided, must be between 1 and 255 characters.
     */
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters if provided")
    private String title;

    /**
     * Author of the book.<br>
     * Optional. If provided, must be between 1 and 255 characters.
     */
    @Size(min = 1, max = 255, message = "Author must be between 1 and 255 characters if provided")
    private String author;

    /**
     * ISBN of the book.<br>
     * Optional. If provided, must be between 10 and 17 characters.
     */
    @Size(min = 10, max = 17, message = "ISBN must be between 10 and 17 characters if provided")
    private String isbn;

    /**
     * Publisher of the book.<br>
     * Optional. If provided, must be between 1 and 255 characters.
     */
    @Size(min = 1, max = 255, message = "Publisher must be between 1 and 255 characters if provided")
    private String publisher;

    /**
     * Genre of the book.<br>
     * Optional. If provided, must be at most 50 characters.
     */
    @Size(max = 50, message = "Genre must not exceed 50 characters if provided")
    private String genre;

    /**
     * Publication date of the book.<br>
     * Optional.
     */
    private LocalDate publicationDate;

    /**
     * Total number of copies.<br>
     * Optional. Must be zero or positive if provided.
     */
    @PositiveOrZero(message = "Total copies must be zero or positive if provided")
    private Integer totalCopies;

    /**
     * Number of available copies.<br>
     * Optional. Must be zero or positive if provided.
     */
    @PositiveOrZero(message = "Available copies must be zero or positive if provided")
    private Integer availableCopies;
}