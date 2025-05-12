package com.librarysystem.authservice.dto;

import lombok.Data;

@Data
public class UserDTO {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String role; // e.g., ADMIN, LIBRARIAN, MEMBER
}