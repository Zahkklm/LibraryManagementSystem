package com.librarysystem.bookservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

/**
 * <p><b>DTO for creating a new book in the library.</b></p>
 * <p>Includes validation annotations for each field.</p>
 */
@Data
public class CreateBookRequest {

    /**
     * <b>Title of the book.</b><br>
     * <i>Required. Maximum 255 characters.</i>
     */
    @NotBlank(message = "Title is mandatory")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    /**
     * <b>Author of the book.</b><br>
     * <i>Required. Maximum 255 characters.</i>
     */
    @NotBlank(message = "Author is mandatory")
    @Size(max = 255, message = "Author cannot exceed 255 characters")
    private String author;

    /**
     * <b>ISBN of the book.</b><br>
     * <i>Required. Must be between 10 (ISBN-10) and 17 (ISBN-13 with hyphens) characters.</i>
     */
    @NotBlank(message = "ISBN is mandatory")
    @Size(min = 10, max = 17, message = "ISBN must be between 10 (ISBN-10) and 17 (ISBN-13 with hyphens) characters")
    private String isbn;

    /**
     * <b>Publisher of the book.</b><br>
     * <i>Required. Maximum 255 characters.</i>
     */
    @NotBlank(message = "Publisher is mandatory")
    @Size(max = 255, message = "Publisher cannot exceed 255 characters")
    private String publisher;

    /**
     * <b>Genre of the book.</b><br>
     * <i>Optional. Maximum 50 characters.</i>
     */
    @Size(max = 50, message = "Genre cannot exceed 50 characters")
    private String genre;

    /**
     * <b>Publication date of the book.</b><br>
     * <i>Optional.</i>
     */
    private LocalDate publicationDate;

    /**
     * <b>Total number of copies.</b><br>
     * <i>Required. Must be zero or positive.</i>
     */
    @NotNull(message = "Total copies is mandatory")
    @PositiveOrZero(message = "Total copies must be zero or positive")
    private Integer totalCopies;

    /**
     * <b>Number of available copies.</b><br>
     * <i>Optional. Must be zero or positive if provided.<br>
     * If null, service will default it to totalCopies.</i>
     */
    @PositiveOrZero(message = "Available copies must be zero or positive if provided")
    private Integer availableCopies;
}