package com.librarysystem.userservice.dto;

import com.librarysystem.userservice.entity.User; // Assuming your User entity is in this package
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for User information.
 * Used to send user data to clients, excluding sensitive information like passwords.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String role; // e.g., ADMIN, LIBRARIAN, MEMBER
    private boolean active;

    /**
     * Static factory method to create a UserDTO from a User entity.
     *
     * @param user The User entity to convert.
     * @return A UserDTO populated with data from the User entity, or null if the input user is null.
     */
    public static UserDTO fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new UserDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null, // Convert enum to String, handle null
                user.isActive()
        );
    }
}