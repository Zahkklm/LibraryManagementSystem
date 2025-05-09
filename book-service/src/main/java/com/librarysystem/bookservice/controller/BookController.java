package com.librarysystem.bookservice.controller;

import com.librarysystem.bookservice.dto.BookResponse;
import com.librarysystem.bookservice.dto.CreateBookRequest;
import com.librarysystem.bookservice.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.librarysystem.bookservice.dto.ErrorResponse; 
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing library book resources.
 * 
 * This controller handles HTTP requests related to book operations in the library system,
 * including creating, retrieving, updating, and deleting books. Access to these operations
 * is restricted based on user roles, enforcing a least-privilege security model:
 * 
 *   Read operations (GET) - Available to all authenticated users (MEMBER, LIBRARIAN, ADMIN)
 *   Write operations (POST, PUT, DELETE) - Restricted to staff with elevated privileges (LIBRARIAN, ADMIN)
 * 
 * 
 * Security is enforced through method-level {@code @PreAuthorize} annotations that rely on
 * the Spring Security context established by {@code RoleExtractionFilter} based on user roles
 * propagated from the API Gateway.
 * 
 * All endpoints under {@code /api/books/**} are protected by {@code GatewayValidationFilter}
 * to ensure requests originate from the trusted API Gateway.
 */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Tag(name = "Book Management", description = "APIs for managing books in the library")
public class BookController {

    private final BookService bookService;

    /**
     * Creates a new book record in the library system.
     * 
     * This endpoint is restricted to librarians and administrators who are authorized
     * to add new books to the library collection. The request is validated to ensure
     * all required fields are present and correctly formatted.
     * 
     * @param createRequest The validated book creation details including title, author, ISBN, etc.
     * @return The created book with HTTP 201 (CREATED) status
     * @throws jakarta.validation.ConstraintViolationException if validation fails
     * @throws com.librarysystem.bookservice.exception.IsbnAlreadyExistsException if a book with the same ISBN already exists
     */
    @Operation(summary = "Add a new book", description = "Creates a new book record in the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Book created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid Gateway Secret",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient privileges",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - Book with ISBN already exists",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('LIBRARIAN', 'ADMIN')")
    public ResponseEntity<BookResponse> addBook(@Valid @RequestBody CreateBookRequest createRequest) {
        BookResponse newBook = bookService.addBook(createRequest);
        return new ResponseEntity<>(newBook, HttpStatus.CREATED);
    }

    /**
     * Retrieves a book by its unique identifier.
     * 
     * This endpoint is available to all authenticated users (members, librarians, and administrators)
     * as part of the library's basic functionality to browse available books.
     * 
     * @param id The unique identifier of the book to retrieve
     * @return The requested book with HTTP 200 (OK) status if found, 
     *         or HTTP 404 (NOT FOUND) if no book exists with the given ID
     */
    @Operation(summary = "Get a book by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Book not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEMBER', 'LIBRARIAN', 'ADMIN')")
    public ResponseEntity<BookResponse> getBookById(@PathVariable Long id) {
        return bookService.getBookById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves a book by its ISBN (International Standard Book Number).
     * 
     * ISBN provides a standardized way to identify books across libraries worldwide.
     * This endpoint allows lookup of books by their ISBN, which is particularly useful
     * for verifying the presence of specific editions in the library's collection.
     * 
     * @param isbn The ISBN of the book to retrieve
     * @return The requested book with HTTP 200 (OK) status if found,
     *         or HTTP 404 (NOT FOUND) if no book exists with the given ISBN
     */
    @Operation(summary = "Get a book by its ISBN")
    @ApiResponses(value = { /* ... */ })
    @GetMapping("/isbn/{isbn}")
    @PreAuthorize("hasAnyRole('MEMBER', 'LIBRARIAN', 'ADMIN')")
    public ResponseEntity<BookResponse> getBookByIsbn(@PathVariable String isbn) {
        return bookService.getBookByIsbn(isbn)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieves all books in the library collection.
     * 
     * This endpoint returns a complete list of all books currently registered in the system.
     * For large libraries, this endpoint might be paginated in future implementations.
     * 
     * @return A list of all books with HTTP 200 (OK) status
     */
    @Operation(summary = "Get all books")
    @ApiResponses(value = { /* ... */ })
    @GetMapping
    @PreAuthorize("hasAnyRole('MEMBER', 'LIBRARIAN', 'ADMIN')")
    public ResponseEntity<List<BookResponse>> getAllBooks() {
        List<BookResponse> books = bookService.getAllBooks();
        return ResponseEntity.ok(books);
    }
}