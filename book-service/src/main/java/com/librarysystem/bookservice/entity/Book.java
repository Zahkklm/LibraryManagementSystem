package com.librarysystem.bookservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * JPA Entity representing a book in the library system.
 * <p>
 * This class is the core domain entity for the Book Service and maps directly
 * to the 'books' table in the database. It contains all metadata and inventory
 * information for books in the library collection.
 * <p>
 * The entity uses JPA annotations to define the database mapping and Lombok
 * annotations to reduce boilerplate code. The class follows the Domain-Driven
 * Design approach where this entity represents a key domain concept in our
 * library management system.
 * <p>
 * Database constraints ensure data integrity:
 * <ul>
 *   <li>ISBN has a unique constraint to prevent duplicate books</li>
 *   <li>Required fields (title, author, isbn) have not-null constraints</li>
 *   <li>The ID is auto-generated as the primary key</li>
 * </ul>
 */
@Entity
@Table(name = "books",
       uniqueConstraints = {@UniqueConstraint(columnNames = {"isbn"})})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    /**
     * Unique identifier and primary key for the book.
     * <p>
     * This ID is automatically generated when a new book is saved to the database.
     * It serves as the primary reference for the book throughout the system.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The title of the book.
     * <p>
     * This field is required and cannot be null. It represents the main
     * identifying name of the book as would appear on its cover.
     */
    @Column(nullable = false)
    private String title;
    
    /**
     * The author(s) of the book.
     * <p>
     * This field is required and cannot be null. For books with multiple authors,
     * this field typically contains a comma-separated or otherwise formatted list.
     */
    @Column(nullable = false)
    private String author;
    
    /**
     * International Standard Book Number (ISBN) uniquely identifying this book.
     * <p>
     * This field is required and must be unique across all books. The ISBN
     * follows international standards (ISBN-10 or ISBN-13) and is used for
     * identifying books globally.
     */
    @Column(nullable = false, unique = true)
    private String isbn;
    
    /**
     * The publisher of the book.
     * <p>
     * This field is optional and represents the company or organization
     * that published the book.
     */
    private String publisher;
    
    /**
     * The literary genre or category of the book.
     * <p>
     * This field helps in categorizing books based on their content type,
     * such as Fiction, Non-fiction, Science Fiction, etc.
     */
    private String genre;
    
    /**
     * The date when the book was published.
     * <p>
     * This field represents the original publication date of the book, not
     * necessarily when a specific edition was published.
     */
    private LocalDate publicationDate;
    
    /**
     * The total number of copies of this book owned by the library.
     * <p>
     * This represents the library's complete inventory of this title,
     * including both available and borrowed copies.
     */
    @Column(nullable = false)
    private Integer totalCopies;
    
    /**
     * The number of copies currently available for borrowing.
     * <p>
     * This number decreases when books are checked out and increases when
     * they are returned. It must always be less than or equal to totalCopies.
     */
    @Column(nullable = false)
    private Integer availableCopies;
    
    /**
     * Pre-persist hook to ensure inventory consistency before saving.
     * <p>
     * This method is automatically called by JPA before a new entity is persisted.
     * It ensures that default values are set and that availableCopies is not
     * greater than totalCopies.
     */
    @PrePersist
    public void prePersist() {
        if (totalCopies == null) {
            totalCopies = 0;
        }
        if (availableCopies == null) {
            availableCopies = totalCopies;
        }
        // Ensure available copies doesn't exceed total copies
        if (availableCopies > totalCopies) {
            availableCopies = totalCopies;
        }
    }
    
    /**
     * Pre-update hook to ensure inventory consistency before updates.
     * <p>
     * This method is automatically called by JPA before an entity is updated.
     * It ensures that inventory constraints are maintained, particularly that
     * availableCopies does not exceed totalCopies.
     */
    @PreUpdate
    public void preUpdate() {
        // Ensure available copies doesn't exceed total copies
        if (availableCopies > totalCopies) {
            availableCopies = totalCopies;
        }
    }
}