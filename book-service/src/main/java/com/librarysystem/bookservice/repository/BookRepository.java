package com.librarysystem.bookservice.repository;

import com.librarysystem.bookservice.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Repository interface for Book entity persistence operations.
 * <p>
 * This repository provides data access methods for the Book domain entity
 * using Spring Data JPA. It handles all database operations including standard
 * CRUD operations inherited from JpaRepository as well as custom finder methods.
 * <p>
 * By extending JpaRepository, this interface automatically gets implementations for:
 * <ul>
 *   <li>save(Book) - Create or update a book</li>
 *   <li>findById(Long) - Find a book by its primary key</li>
 *   <li>findAll() - Retrieve all books</li>
 *   <li>deleteById(Long) - Delete a book by its ID</li>
 *   <li>existsById(Long) - Check if a book with given ID exists</li>
 *   <li>count() - Get the total number of books</li>
 *   <li>And many other standard repository methods</li>
 * </ul>
 * <p>
 * This repository is used by the BookService to perform data persistence operations
 * without having to interact directly with the database or write SQL queries.
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    
    /**
     * Finds a book by its ISBN (International Standard Book Number).
     * <p>
     * This method uses Spring Data JPA's method naming convention to automatically
     * generate a query that searches for books with a matching ISBN. This allows
     * the service layer to look up books by their industry-standard identifier.
     * <p>
     * The method returns an Optional which will be empty if no book with the
     * specified ISBN exists in the database.
     *
     * @param isbn The ISBN to search for (e.g., "978-3-16-148410-0")
     * @return An Optional containing the book if found, or empty if not found
     */
    Optional<Book> findByIsbn(String isbn);
    
    // TODO: Add findByTitleContaining, findByAuthor
    // Future enhancement: Add search capabilities for finding books by title or author
    // Examples:
    // List<Book> findByTitleContainingIgnoreCase(String titlePart);
    // List<Book> findByAuthorContainingIgnoreCase(String authorName);

     /**
     * Searches for books across multiple fields using a single query string.
     * Performs case-insensitive partial matching on title, author, ISBN, and genre.
     *
     * @param query The search query to look for in multiple fields
     * @param pageable Pagination and sorting parameters
     * @return A page of books matching the search criteria
     */
    @Query("SELECT b FROM Book b WHERE " +
           "LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.isbn) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.genre) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Book> searchBooks(@Param("query") String query, Pageable pageable);
    
    /**
     * Performs an advanced search with separate criteria for each field.
     * Any field can be null, in which case it won't be included in the search criteria.
     *
     * @param title Optional title to search for
     * @param author Optional author to search for
     * @param isbn Optional ISBN to search for
     * @param genre Optional genre to search for
     * @param pageable Pagination and sorting parameters
     * @return A page of books matching the combined search criteria
     */
    @Query("SELECT b FROM Book b WHERE " +
           "(:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:author IS NULL OR LOWER(b.author) LIKE LOWER(CONCAT('%', :author, '%'))) AND " +
           "(:isbn IS NULL OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :isbn, '%'))) AND " +
           "(:genre IS NULL OR LOWER(b.genre) LIKE LOWER(CONCAT('%', :genre, '%')))")
    Page<Book> advancedSearch(@Param("title") String title, 
                             @Param("author") String author, 
                             @Param("isbn") String isbn, 
                             @Param("genre") String genre, 
                             Pageable pageable);
}