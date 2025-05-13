package com.librarysystem.bookservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.librarysystem.bookservice.config.GatewayValidationFilter;
import com.librarysystem.bookservice.config.RoleExtractionFilter;
import com.librarysystem.bookservice.config.TestSecurityConfig;
import com.librarysystem.bookservice.dto.BookResponse;
import com.librarysystem.bookservice.dto.CreateBookRequest;
import com.librarysystem.bookservice.dto.UpdateBookRequest;
import com.librarysystem.bookservice.service.BookService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BookController.class)
@Import(TestSecurityConfig.class) // Ensure this enables method security for @PreAuthorize
public class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GatewayValidationFilter gatewayValidationFilter;

    @MockBean
    private RoleExtractionFilter roleExtractionFilter;

    private BookResponse sampleBookResponse;
    private CreateBookRequest validCreateBookRequest;
    private UpdateBookRequest validUpdateBookRequest;
    private final String gatewaySecret = "test-secret"; // Use a consistent test secret

    private final String LIBRARIAN_ROLE = "LIBRARIAN";
    private final String ADMIN_ROLE = "ADMIN";
    private final String MEMBER_ROLE = "MEMBER";

    private final String LIBRARIAN_ID = "librarian-user-id";
    private final String ADMIN_ID = "admin-user-id";
    private final String MEMBER_ID = "member-user-id";
    private final String ANONYMOUS_ID = "anonymous-test-user";


    @BeforeEach
    void setUp() throws ServletException, IOException {
        sampleBookResponse = BookResponse.builder()
                .id(1L)
                .title("Effective Java")
                .author("Joshua Bloch")
                .isbn("978-0134685991")
                .publisher("Addison-Wesley")
                .publicationDate(LocalDate.of(2018, 1, 6))
                .totalCopies(10)
                .availableCopies(7)
                .build();

        validCreateBookRequest = new CreateBookRequest();
        validCreateBookRequest.setTitle("Clean Code");
        validCreateBookRequest.setAuthor("Robert C. Martin");
        validCreateBookRequest.setIsbn("978-0132350884"); // Valid ISBN
        validCreateBookRequest.setPublisher("Prentice Hall");
        validCreateBookRequest.setPublicationDate(LocalDate.of(2008, 8, 1));
        validCreateBookRequest.setTotalCopies(5);
        validCreateBookRequest.setAvailableCopies(5);

        validUpdateBookRequest = new UpdateBookRequest();
        validUpdateBookRequest.setTitle("Effective Java (3rd Edition)");
        validUpdateBookRequest.setAuthor("Joshua Bloch");
        validUpdateBookRequest.setIsbn("978-0134685991"); // Valid ISBN
        validUpdateBookRequest.setPublisher("Pearson");
        validUpdateBookRequest.setPublicationDate(LocalDate.of(2018, 1, 7));
        validUpdateBookRequest.setTotalCopies(12);
        validUpdateBookRequest.setAvailableCopies(6);

        // Mock GatewayValidationFilter to proceed
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(gatewayValidationFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));

        // Mock RoleExtractionFilter to simulate role extraction and SecurityContext setup
        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);

            String rolesHeader = request.getHeader("X-User-Roles");
            String userIdHeader = request.getHeader("X-User-Id");

            String principalName = (userIdHeader != null && !userIdHeader.isEmpty()) ? userIdHeader : ANONYMOUS_ID;
            List<GrantedAuthority> authorities = new ArrayList<>();

            if (rolesHeader != null && !rolesHeader.isEmpty()) {
                authorities = Arrays.stream(rolesHeader.split(","))
                        .map(role -> "ROLE_" + role.trim().toUpperCase())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }
            // Always set an authentication object.
            // If rolesHeader was null/empty, authorities will be empty, leading to AccessDeniedException for @PreAuthorize.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principalName, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            try {
                chain.doFilter(request, response);
            } finally {
                SecurityContextHolder.clearContext(); // Clean up context after the request
            }
            return null;
        }).when(roleExtractionFilter).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class), any(FilterChain.class));
    }

    private ResultActions performPostWithRole(String url, Object body, String userId, String roles) throws Exception {
        return mockMvc.perform(post(url)
                .header("X-Gateway-Secret", gatewaySecret)
                .header("X-User-Id", userId)
                .header("X-User-Roles", roles)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }
    
    private ResultActions performPostWithoutRole(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .header("X-Gateway-Secret", gatewaySecret)
                // No X-User-Id or X-User-Roles
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions performPutWithRole(String url, Object body, String userId, String roles) throws Exception {
        return mockMvc.perform(put(url)
                .header("X-Gateway-Secret", gatewaySecret)
                .header("X-User-Id", userId)
                .header("X-User-Roles", roles)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }
    
    private ResultActions performPutWithoutRole(String url, Object body) throws Exception {
        return mockMvc.perform(put(url)
                .header("X-Gateway-Secret", gatewaySecret)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions performDeleteWithRole(String url, String userId, String roles) throws Exception {
        return mockMvc.perform(delete(url)
                .header("X-Gateway-Secret", gatewaySecret)
                .header("X-User-Id", userId)
                .header("X-User-Roles", roles));
    }

    private ResultActions performDeleteWithoutRole(String url) throws Exception {
        return mockMvc.perform(delete(url)
                .header("X-Gateway-Secret", gatewaySecret));
    }
    
    private ResultActions performGetWithRole(String url, String userId, String roles) throws Exception {
        return mockMvc.perform(get(url)
                .header("X-Gateway-Secret", gatewaySecret)
                .header("X-User-Id", userId)
                .header("X-User-Roles", roles)
                .accept(MediaType.APPLICATION_JSON));
    }

    private ResultActions performGetWithoutRole(String url) throws Exception {
        return mockMvc.perform(get(url)
                .header("X-Gateway-Secret", gatewaySecret)
                .accept(MediaType.APPLICATION_JSON));
    }

    // --- Add Book Tests (POST /api/books) ---
    @Test
    @DisplayName("POST /api/books - Add Book - Success (Librarian)")
    void addBook_whenValidRequestAndLibrarian_shouldReturnCreated() throws Exception {
        when(bookService.addBook(any(CreateBookRequest.class))).thenReturn(sampleBookResponse);

        performPostWithRole("/api/books", validCreateBookRequest, LIBRARIAN_ID, LIBRARIAN_ROLE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(sampleBookResponse.getId().intValue())))
                .andExpect(jsonPath("$.title", is(sampleBookResponse.getTitle())));
        verify(bookService).addBook(any(CreateBookRequest.class));
    }

    @Test
    @DisplayName("POST /api/books - Add Book - Success (Admin)")
    void addBook_whenValidRequestAndAdmin_shouldReturnCreated() throws Exception {
        when(bookService.addBook(any(CreateBookRequest.class))).thenReturn(sampleBookResponse);

        performPostWithRole("/api/books", validCreateBookRequest, ADMIN_ID, ADMIN_ROLE)
                .andExpect(status().isCreated());
        verify(bookService).addBook(any(CreateBookRequest.class));
    }

    @Test
    @DisplayName("POST /api/books - Add Book - Forbidden (Member)")
    void addBook_whenUserIsMember_shouldReturnForbidden() throws Exception {
        performPostWithRole("/api/books", validCreateBookRequest, MEMBER_ID, MEMBER_ROLE)
                .andExpect(status().isForbidden());
        verify(bookService, never()).addBook(any(CreateBookRequest.class));
    }
    
    @Test
    @DisplayName("POST /api/books - Add Book - Forbidden (No Role/Unauthenticated)")
    void addBook_whenNoRole_shouldReturnForbidden() throws Exception {
        performPostWithoutRole("/api/books", validCreateBookRequest)
                .andExpect(status().isForbidden()); // Expecting 403 due to @PreAuthorize
        verify(bookService, never()).addBook(any(CreateBookRequest.class));
    }

    @Test
    @DisplayName("POST /api/books - Add Book - ISBN Conflict (Librarian)")
    void addBook_whenIsbnExists_shouldReturnConflict() throws Exception {
        when(bookService.addBook(any(CreateBookRequest.class)))
                .thenThrow(new IllegalArgumentException("Book with ISBN " + validCreateBookRequest.getIsbn() + " already exists."));

        performPostWithRole("/api/books", validCreateBookRequest, LIBRARIAN_ID, LIBRARIAN_ROLE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Book with ISBN " + validCreateBookRequest.getIsbn() + " already exists.")));
    }

    @Test
    @DisplayName("POST /api/books - Add Book - Validation Error (Librarian)")
    void addBook_whenInvalidRequest_shouldReturnBadRequest() throws Exception {
        CreateBookRequest invalidRequest = new CreateBookRequest(); // Missing all mandatory fields

        performPostWithRole("/api/books", invalidRequest, LIBRARIAN_ID, LIBRARIAN_ROLE)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Input validation failed for one or more fields.")))
                .andExpect(jsonPath("$.details", hasSize(5))) // title, author, isbn, publisher, totalCopies
                .andExpect(jsonPath("$.details", containsInAnyOrder(
                        "title: Title is mandatory",
                        "author: Author is mandatory",
                        "isbn: ISBN is mandatory",
                        "publisher: Publisher is mandatory",
                        "totalCopies: Total copies is mandatory"
                )));
    }
    
    @Test
    @DisplayName("POST /api/books - Add Book - CreateRequest Missing Publisher - Bad Request (Librarian)")
    void addBook_whenCreateRequestMissingPublisher_shouldReturnBadRequest() throws Exception {
        CreateBookRequest requestWithMissingPublisher = new CreateBookRequest();
        requestWithMissingPublisher.setTitle("Valid Title");
        requestWithMissingPublisher.setAuthor("Valid Author");
        requestWithMissingPublisher.setIsbn("1234567890"); // Valid ISBN
        // Publisher is missing
        requestWithMissingPublisher.setTotalCopies(10);

        performPostWithRole("/api/books", requestWithMissingPublisher, LIBRARIAN_ID, LIBRARIAN_ROLE)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Input validation failed for one or more fields.")))
                .andExpect(jsonPath("$.details", hasSize(1))) 
                .andExpect(jsonPath("$.details[0]", is("publisher: Publisher is mandatory")));
    }

    // --- Get Book By Id Tests (GET /api/books/{id}) ---
    @Test
    @DisplayName("GET /api/books/{id} - Get Book By Id - Found (Member)")
    void getBookById_whenBookExistsAndMember_shouldReturnOk() throws Exception {
        when(bookService.getBookById(1L)).thenReturn(Optional.of(sampleBookResponse));
        performGetWithRole("/api/books/1", MEMBER_ID, MEMBER_ROLE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is(sampleBookResponse.getTitle())));
        verify(bookService).getBookById(1L);
    }

    @Test
    @DisplayName("GET /api/books/{id} - Get Book By Id - Not Found (Member)")
    void getBookById_whenBookNotExists_shouldReturnNotFound() throws Exception {
        when(bookService.getBookById(99L)).thenReturn(Optional.empty());
        performGetWithRole("/api/books/99", MEMBER_ID, MEMBER_ROLE)
                .andExpect(status().isNotFound());
        verify(bookService).getBookById(99L);
    }

    @Test
    @DisplayName("GET /api/books/{id} - Get Book By Id - Forbidden (No Role/Unauthenticated)")
    void getBookById_whenNoRole_shouldReturnForbidden() throws Exception {
        performGetWithoutRole("/api/books/1")
                .andExpect(status().isForbidden());
        verify(bookService, never()).getBookById(anyLong());
    }


    // --- Get Book By ISBN Tests (GET /api/books/isbn/{isbn}) ---
    @Test
    @DisplayName("GET /api/books/isbn/{isbn} - Get Book By ISBN - Found (Librarian)")
    void getBookByIsbn_whenBookExistsAndLibrarian_shouldReturnOk() throws Exception {
        when(bookService.getBookByIsbn(sampleBookResponse.getIsbn())).thenReturn(Optional.of(sampleBookResponse));
        performGetWithRole("/api/books/isbn/" + sampleBookResponse.getIsbn(), LIBRARIAN_ID, LIBRARIAN_ROLE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is(sampleBookResponse.getTitle())));
        verify(bookService).getBookByIsbn(sampleBookResponse.getIsbn());
    }

    @Test
    @DisplayName("GET /api/books/isbn/{isbn} - Get Book By ISBN - Not Found (Admin)")
    void getBookByIsbn_whenBookNotExistsAndAdmin_shouldReturnNotFound() throws Exception {
        String nonExistentIsbn = "000-0000000000";
        when(bookService.getBookByIsbn(nonExistentIsbn)).thenReturn(Optional.empty());
        performGetWithRole("/api/books/isbn/" + nonExistentIsbn, ADMIN_ID, ADMIN_ROLE)
                .andExpect(status().isNotFound());
        verify(bookService).getBookByIsbn(nonExistentIsbn);
    }
    
    @Test
    @DisplayName("GET /api/books/isbn/{isbn} - Get Book By ISBN - Forbidden (No Role/Unauthenticated)")
    void getBookByIsbn_whenNoRole_shouldReturnForbidden() throws Exception {
        performGetWithoutRole("/api/books/isbn/" + sampleBookResponse.getIsbn())
                .andExpect(status().isForbidden());
        verify(bookService, never()).getBookByIsbn(anyString());
    }

    // --- Get All Books Tests (GET /api/books) ---
    @Test
    @DisplayName("GET /api/books - Get All Books - Success (Member)")
    void getAllBooks_whenBooksExistAndMember_shouldReturnOkWithListOfBooks() throws Exception {
        when(bookService.getAllBooks()).thenReturn(List.of(sampleBookResponse));
        performGetWithRole("/api/books", MEMBER_ID, MEMBER_ROLE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is(sampleBookResponse.getTitle())));
        verify(bookService).getAllBooks();
    }

    @Test
    @DisplayName("GET /api/books - Get All Books - No Books Found (Admin)")
    void getAllBooks_whenNoBooksExistAndAdmin_shouldReturnOkWithEmptyList() throws Exception {
        when(bookService.getAllBooks()).thenReturn(Collections.emptyList());
        performGetWithRole("/api/books", ADMIN_ID, ADMIN_ROLE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        verify(bookService).getAllBooks();
    }

    @Test
    @DisplayName("GET /api/books - Get All Books - Forbidden (No Role/Unauthenticated)")
    void getAllBooks_whenNoRole_shouldReturnForbidden() throws Exception {
        performGetWithoutRole("/api/books")
                .andExpect(status().isForbidden());
        verify(bookService, never()).getAllBooks();
    }

    // --- Update Book Tests (PUT /api/books/{id}) ---
    @Test
    @DisplayName("PUT /api/books/{id} - Update Book - Success (Librarian)")
    void updateBook_whenValidRequestAndBookExistsAndLibrarian_shouldReturnOk() throws Exception {
        BookResponse updatedResponse = BookResponse.builder()
                .id(1L).title(validUpdateBookRequest.getTitle()).author(validUpdateBookRequest.getAuthor())
                .isbn(validUpdateBookRequest.getIsbn()).publisher(validUpdateBookRequest.getPublisher())
                .publicationDate(validUpdateBookRequest.getPublicationDate())
                .totalCopies(validUpdateBookRequest.getTotalCopies()).availableCopies(validUpdateBookRequest.getAvailableCopies())
                .build();
        when(bookService.updateBook(eq(1L), any(UpdateBookRequest.class))).thenReturn(updatedResponse);

        performPutWithRole("/api/books/1", validUpdateBookRequest, LIBRARIAN_ID, LIBRARIAN_ROLE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is(updatedResponse.getTitle())));
        verify(bookService).updateBook(eq(1L), any(UpdateBookRequest.class));
    }

    @Test
    @DisplayName("PUT /api/books/{id} - Update Book - Forbidden (Member)")
    void updateBook_whenUserIsMember_shouldReturnForbidden() throws Exception {
        performPutWithRole("/api/books/1", validUpdateBookRequest, MEMBER_ID, MEMBER_ROLE)
                .andExpect(status().isForbidden());
        verify(bookService, never()).updateBook(anyLong(), any(UpdateBookRequest.class));
    }

    @Test
    @DisplayName("PUT /api/books/{id} - Update Book - Forbidden (No Role/Unauthenticated)")
    void updateBook_whenNoRole_shouldReturnForbidden() throws Exception {
        performPutWithoutRole("/api/books/1", validUpdateBookRequest)
                .andExpect(status().isForbidden());
        verify(bookService, never()).updateBook(anyLong(), any(UpdateBookRequest.class));
    }

    @Test
    @DisplayName("PUT /api/books/{id} - Update Book - Not Found (Admin)")
    void updateBook_whenBookNotFoundAndAdmin_shouldReturnNotFound() throws Exception {
        when(bookService.updateBook(eq(99L), any(UpdateBookRequest.class)))
                .thenThrow(new RuntimeException("Book not found with id: 99"));

        performPutWithRole("/api/books/99", validUpdateBookRequest, ADMIN_ID, ADMIN_ROLE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Book not found with id: 99")));
    }

    @Test
    @DisplayName("PUT /api/books/{id} - Update Book - ISBN Conflict (Librarian)")
    void updateBook_whenIsbnConflict_shouldReturnConflict() throws Exception {
        UpdateBookRequest conflictingUpdateRequest = new UpdateBookRequest();
        // Populate with valid data except for the conflicting ISBN
        conflictingUpdateRequest.setTitle("Another Valid Title");
        conflictingUpdateRequest.setAuthor("Another Valid Author");
        conflictingUpdateRequest.setPublisher("Another Valid Publisher");
        conflictingUpdateRequest.setTotalCopies(5);
        String conflictingIsbn = "111222333X"; // Valid format, but will conflict
        conflictingUpdateRequest.setIsbn(conflictingIsbn);


        when(bookService.updateBook(eq(1L), any(UpdateBookRequest.class)))
                .thenThrow(new IllegalArgumentException("Another book with ISBN " + conflictingIsbn + " already exists."));

        performPutWithRole("/api/books/1", conflictingUpdateRequest, LIBRARIAN_ID, LIBRARIAN_ROLE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Another book with ISBN " + conflictingIsbn + " already exists.")));
    }

    @Test
    @DisplayName("PUT /api/books/{id} - Update Book - Validation Error (Librarian)")
    void updateBook_whenInvalidRequest_shouldReturnBadRequest() throws Exception {
        UpdateBookRequest invalidRequest = new UpdateBookRequest();
        invalidRequest.setTitle(""); // Violates @Size(min=1) if present in UpdateBookRequest
                                     // Or make ISBN invalid, e.g., too short
        invalidRequest.setIsbn("123"); // Too short for @Size(min=10, max=17)

        performPutWithRole("/api/books/1", invalidRequest, LIBRARIAN_ID, LIBRARIAN_ROLE)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Input validation failed for one or more fields.")))
                .andExpect(jsonPath("$.details", hasSize(2))) // Expecting errors for title and isbn
                .andExpect(jsonPath("$.details", containsInAnyOrder(
                    "title: Title must be between 1 and 255 characters if provided", // Assuming this validation exists
                    "isbn: ISBN must be between 10 and 17 characters if provided"
                )));
    }

    // --- Delete Book Tests (DELETE /api/books/{id}) ---
    @Test
    @DisplayName("DELETE /api/books/{id} - Delete Book - Success (Admin)")
    void deleteBook_whenBookExistsAndAdmin_shouldReturnNoContent() throws Exception {
        Mockito.doNothing().when(bookService).deleteBook(1L);
        performDeleteWithRole("/api/books/1", ADMIN_ID, ADMIN_ROLE)
                .andExpect(status().isNoContent());
        verify(bookService).deleteBook(1L);
    }

    @Test
    @DisplayName("DELETE /api/books/{id} - Delete Book - Forbidden (Member)")
    void deleteBook_whenUserIsMember_shouldReturnForbidden() throws Exception {
        performDeleteWithRole("/api/books/1", MEMBER_ID, MEMBER_ROLE)
                .andExpect(status().isForbidden());
        verify(bookService, never()).deleteBook(anyLong());
    }

    @Test
    @DisplayName("DELETE /api/books/{id} - Delete Book - Forbidden (No Role/Unauthenticated)")
    void deleteBook_whenNoRole_shouldReturnForbidden() throws Exception {
        performDeleteWithoutRole("/api/books/1")
                .andExpect(status().isForbidden());
        verify(bookService, never()).deleteBook(anyLong());
    }

    @Test
    @DisplayName("DELETE /api/books/{id} - Delete Book - Not Found (Librarian)")
    void deleteBook_whenBookNotFoundAndLibrarian_shouldReturnNotFound() throws Exception {
        // BookService.deleteBook throws RuntimeException if not found
        doThrow(new RuntimeException("Book not found with id: 99")).when(bookService).deleteBook(99L);
        performDeleteWithRole("/api/books/99", LIBRARIAN_ID, LIBRARIAN_ROLE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Book not found with id: 99")));
        verify(bookService).deleteBook(99L);
    }
}