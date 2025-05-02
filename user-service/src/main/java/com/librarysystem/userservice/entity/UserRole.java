package com.librarysystem.userservice.entity;

/**
 * Enumeration of possible user roles in the library management system.
 * These roles determine the access levels and permissions of users.
 */
public enum UserRole {
    /**
     * System administrator role.
     * Has full access to all system functions including:
     * - User management
     * - System configuration
     * - Access to all reports and analytics
     * - Managing other administrators
     */
    ADMIN,

    /**
     * Library staff role.
     * Has access to library operation functions including:
     * - Book management
     * - User management (except administrators)
     * - Borrowing operations
     * - Basic reports
     */
    LIBRARIAN,

    /**
     * Basic library member role.
     * Has access to basic library functions including:
     * - Viewing available books
     * - Borrowing books
     * - Managing own profile
     * - Viewing personal borrowing history
     */
    MEMBER
}