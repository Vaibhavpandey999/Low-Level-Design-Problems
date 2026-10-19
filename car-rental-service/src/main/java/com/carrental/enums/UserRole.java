package com.carrental.enums;

/**
 * UserRole – defines the access level for a registered user.
 *
 * CUSTOMER : standard user who can browse, book, cancel rentals
 * ADMIN    : platform operator who can manage fleet, branches, and view reports
 *
 * Used in @PreAuthorize annotations on controller endpoints.
 */
public enum UserRole {
    CUSTOMER,  // regular end-user
    ADMIN      // back-office operator with elevated privileges
}

