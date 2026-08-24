package com.vehiclerental.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

/**
 * Represents a registered user in the system.
 * A user can be either an ADMIN or a CUSTOMER.
 * The password field is excluded from JSON serialization for security.
 */
@Entity
@Table(name = "users",
    indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true)
    }
)
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Full display name */
    @Column(nullable = false)
    private String name;

    /** Unique email — used as login credential */
    @Column(nullable = false, unique = true)
    private String email;

    /** Hashed/plain password — NEVER returned in API responses */
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    /** Driving license number (required for customers) */
    private String licenseNumber;

    /** Role determines which dashboard the user accesses */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}
