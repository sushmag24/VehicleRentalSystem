package com.vehiclerental.service;

import com.vehiclerental.model.Role;
import com.vehiclerental.model.User;
import com.vehiclerental.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Business logic for user registration, authentication, and retrieval.
 *
 * NOTE (MVP): Passwords are stored in plain text for simplicity.
 * In production, use BCryptPasswordEncoder or similar.
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // ─────────────────────────────────────────────
    //  REGISTRATION
    // ─────────────────────────────────────────────

    /**
     * Registers a new CUSTOMER account.
     * Throws if the email is already in use.
     */
    public User registerUser(String name, String email, String password, String licenseNumber) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email '" + email + "' is already registered.");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password); // In production: BCrypt.hash(password)
        user.setLicenseNumber(licenseNumber);
        user.setRole(Role.CUSTOMER);
        return userRepository.save(user);
    }

    // ─────────────────────────────────────────────
    //  AUTHENTICATION
    // ─────────────────────────────────────────────

    /**
     * Validates email + password combination.
     * Returns the authenticated User or throws on failure.
     */
    public User authenticateUser(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("No account found with email: " + email);
        }

        User user = userOpt.get();
        // In production: use BCryptPasswordEncoder.matches(password, user.getPassword())
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Invalid password. Please try again.");
        }

        return user;
    }

    // ─────────────────────────────────────────────
    //  RETRIEVAL
    // ─────────────────────────────────────────────

    /** Returns a user by ID, or throws if not found */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User with ID " + id + " not found."));
    }

    /** Returns all registered users (admin use only) */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
