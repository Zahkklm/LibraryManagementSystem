package com.librarysystem.bookservice.service;

import com.librarysystem.bookservice.dto.BookResponse;
import com.librarysystem.bookservice.dto.CreateBookRequest;
import com.librarysystem.bookservice.dto.UpdateBookRequest;
import com.librarysystem.bookservice.entity.Book;
import com.librarysystem.bookservice.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class that implements the core business logic for book management.
 * <p>
 * This service acts as an intermediary between the controllers and the data access layer,
 * handling operations such as creating, retrieving, updating, and deleting books.
 * It enforces business rules such as ISBN uniqueness and inventory constraints,
 * and performs necessary transformations between DTOs and domain entities.
 * <p>
 * All data persistence operations are delegated to the {@link BookRepository},
 * following the principle of separation of concerns.
 */
@Service
@RequiredArgsConstructor
public class BookService {

    private static final Logger logger = LoggerFactory.getLogger(BookService.class);
    
    /**
     * Repository for Book entity persistence operations.
     * <p>
     * This repository provides data access methods for the Book domain entity,
     * abstracting the underlying database operations. It's injected through
     * constructor injection via Lombok's {@code @RequiredArgsConstructor}.
     * <p>
     * Key operations performed through this repository include:
     * <ul>
     *   <li>Finding books by ID or ISBN</li>
     *   <li>Retrieving all books in the library</li>
     *   <li>Persisting new book records</li>
     *   <li>Updating existing book information</li>
     *   <li>Removing books from the collection</li>
     * </ul>
     * <p>
     * The repository interface extends Spring Data JPA's repository interfaces,
     * providing standard CRUD operations and custom finder methods.
     */
    private final BookRepository bookRepository;

    /**
     * Converts a Book entity to a BookResponse DTO.
     * <p>
     * This mapper method transforms the internal domain model representation
     * into a data transfer object suitable for API responses, ensuring that
     * only the necessary data is exposed to the client.
     * 
     * @param book The Book entity to convert
     * @return A BookResponse DTO containing the book's public information
     */
    private BookResponse toBookResponse(Book book) {
        if (book == null) return null;
        return BookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .publisher(book.getPublisher())
                .genre(book.getGenre())
                .publicationDate(book.getPublicationDate())
                .availableCopies(book.getAvailableCopies())
                .totalCopies(book.getTotalCopies())
                .build();
    }

    /**
     * Converts a CreateBookRequest DTO to a Book entity.
     * <p>
     * This mapper method transforms client input into the domain model,
     * applying default values where appropriate. For example, if
     * availableCopies is not specified, it defaults to the total copies.
     * 
     * @param request The CreateBookRequest containing book details from the client
     * @return A new Book entity initialized with the request data
     */
    private Book toBookEntity(CreateBookRequest request) {
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setPublisher(request.getPublisher());
        book.setGenre(request.getGenre());
        book.setPublicationDate(request.getPublicationDate());
        book.setTotalCopies(request.getTotalCopies());
        book.setAvailableCopies(request.getAvailableCopies() != null ? 
                request.getAvailableCopies() : request.getTotalCopies());
        return book;
    }

    // Other methods remain the same

    /**
     * Updates an existing book's information.
     */
    @Transactional
    public BookResponse updateBook(Long id, UpdateBookRequest updateRequest) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));

        // Check for ISBN conflict if ISBN is being changed
        if (updateRequest.getIsbn() != null && !updateRequest.getIsbn().equals(book.getIsbn())) {
            bookRepository.findByIsbn(updateRequest.getIsbn()).ifPresent(existing -> {
                throw new IllegalArgumentException("Another book with ISBN " + 
                        updateRequest.getIsbn() + " already exists.");
            });
            book.setIsbn(updateRequest.getIsbn());
        }

        // Update only non-null fields
        if (updateRequest.getTitle() != null) book.setTitle(updateRequest.getTitle());
        if (updateRequest.getAuthor() != null) book.setAuthor(updateRequest.getAuthor());
        if (updateRequest.getPublisher() != null) book.setPublisher(updateRequest.getPublisher());
        if (updateRequest.getGenre() != null) book.setGenre(updateRequest.getGenre());
        if (updateRequest.getPublicationDate() != null) book.setPublicationDate(updateRequest.getPublicationDate());
        if (updateRequest.getTotalCopies() != null) book.setTotalCopies(updateRequest.getTotalCopies());
        if (updateRequest.getAvailableCopies() != null) book.setAvailableCopies(updateRequest.getAvailableCopies());

        logger.info("Updating book with ID: {}", id);
        return toBookResponse(bookRepository.save(book));
    }

    /**
     * Adds a new book to the library collection.
     * <p>
     * This method performs the following operations:
     * <ol>
     *   <li>Validates that the ISBN doesn't already exist in the repository</li>
     *   <li>Converts the request DTO to a Book entity</li>
     *   <li>Sets default values for nulls in inventory counts</li>
     *   <li>Persists the new book via the repository</li>
     *   <li>Returns the created book as a response DTO</li>
     * </ol>
     *
     * @param createRequest The validated request containing book details
     * @return A BookResponse containing the saved book's information
     * @throws IllegalArgumentException if a book with the same ISBN already exists
     */
    @Transactional
    public BookResponse addBook(CreateBookRequest createRequest) {
        if (bookRepository.findByIsbn(createRequest.getIsbn()).isPresent()) {
            logger.warn("Attempt to add book with existing ISBN: {}", createRequest.getIsbn());
            throw new IllegalArgumentException("Book with ISBN " + createRequest.getIsbn() + " already exists.");
        }
        
        Book book = toBookEntity(createRequest);
        // Ensure proper inventory counts
        if (book.getAvailableCopies() == null && book.getTotalCopies() != null) {
            book.setAvailableCopies(book.getTotalCopies());
        } else if (book.getAvailableCopies() == null) {
            book.setAvailableCopies(0);
        }
        if (book.getTotalCopies() == null && book.getAvailableCopies() != null) {
            book.setTotalCopies(book.getAvailableCopies());
        } else if (book.getTotalCopies() == null) {
            book.setTotalCopies(0);
        }

        logger.info("Adding new book: {}", book.getTitle());
        return toBookResponse(bookRepository.save(book));
    }

    /**
     * Retrieves a book by its unique identifier.
     * <p>
     * Uses the repository's findById method to locate the book, then
     * transforms it to a DTO if found.
     *
     * @param id The unique identifier of the book
     * @return An Optional containing the book if found, or empty if not found
     */
    public Optional<BookResponse> getBookById(Long id) {
        return bookRepository.findById(id).map(this::toBookResponse);
    }

    /**
     * Retrieves a book by its ISBN (International Standard Book Number).
     * <p>
     * Uses the repository's custom findByIsbn method to locate the book,
     * then transforms it to a DTO if found.
     *
     * @param isbn The ISBN of the book to find
     * @return An Optional containing the book if found, or empty if not found
     */
    public Optional<BookResponse> getBookByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn).map(this::toBookResponse);
    }

    /**
     * Retrieves all books in the library collection with pagination support.
     * <p>
     * Fetches a page of book entities from the repository based on the provided
     * pagination parameters, and transforms each one to a DTO for the response.
     *
     * @param pageable Pagination information including page number, size, and sorting
     * @return A page of books as BookResponse DTOs
     */
    public Page<BookResponse> getAllBooks(Pageable pageable) {
        logger.info("Fetching books page {} with size {}", 
                   pageable.getPageNumber(), pageable.getPageSize());
        return bookRepository.findAll(pageable)
                .map(this::toBookResponse);
    }
    
    /**
     * Retrieves all books in the library collection without pagination.
     * <p>
     * This method is kept for backward compatibility but should be used carefully
     * with large datasets as it fetches all records at once.
     *
     * @return A list of all books as BookResponse DTOs
     */
    public List<BookResponse> getAllBooks() {
        logger.info("Fetching all books (unpaginated)");
        return bookRepository.findAll().stream()
                .map(this::toBookResponse)
                .collect(Collectors.toList());
    }

    /**
     * Deletes a book from the library collection.
     * <p>
     * Verifies the book exists before attempting deletion to provide
     * clearer error reporting. The deletion is performed via the repository.
     *
     * @param id The unique identifier of the book to delete
     * @throws RuntimeException if no book exists with the given ID
     */
    @Transactional
    public void deleteBook(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new RuntimeException("Book not found with id: " + id);
        }
        logger.info("Deleting book with ID: {}", id);
        bookRepository.deleteById(id);
    }

    /**
     * Searches for books across multiple fields (title, author, ISBN, genre).
     * <p>
     * This method performs a case-insensitive partial match search on
     * the specified fields and returns paginated results.
     *
     * @param query Search query string to look for
     * @param pageable Pagination information including page number, size, and sorting
     * @return A page of matching books as BookResponse DTOs
     */
    public Page<BookResponse> searchBooks(String query, Pageable pageable) {
        logger.info("Searching for books matching query: '{}', page {}, size {}", 
                    query, pageable.getPageNumber(), pageable.getPageSize());
        return bookRepository.searchBooks(query, pageable)
                .map(this::toBookResponse);
    }

    /**
     * Performs an advanced search with separate criteria for each field.
     * <p>
     * This method allows for more targeted searches where users can specify
     * which fields to search in. Any parameter can be null, in which case
     * it won't be included in the search criteria.
     *
     * @param title Optional title to search for
     * @param author Optional author to search for
     * @param isbn Optional ISBN to search for
     * @param genre Optional genre to search for
     * @param pageable Pagination information including page number, size, and sorting
     * @return A page of matching books as BookResponse DTOs
     */
    public Page<BookResponse> advancedSearch(String title, String author, 
                                            String isbn, String genre, 
                                            Pageable pageable) {
        logger.info("Advanced search - Title: '{}', Author: '{}', ISBN: '{}', Genre: '{}'", 
                title, author, isbn, genre);
        return bookRepository.advancedSearch(title, author, isbn, genre, pageable)
                .map(this::toBookResponse);
    }
}