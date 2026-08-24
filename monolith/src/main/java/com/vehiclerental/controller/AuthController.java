package com.vehiclerental.controller;

import com.vehiclerental.dto.AuthRequest;
import com.vehiclerental.dto.AuthResponse;
import com.vehiclerental.dto.RegisterRequest;
import com.vehiclerental.model.User;
import com.vehiclerental.security.JwtUtil;
import com.vehiclerental.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Handles user registration and login.
 *
 * POST /api/auth/register  — create a new CUSTOMER account
 * POST /api/auth/login     — authenticate and receive JWT
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private UserService userService;
    @Autowired private JwtUtil     jwtUtil;

    /**
     * Register a new customer.
     * Roles are always CUSTOMER on self-registration.
     * To create an ADMIN, insert directly into the database (see schema.sql).
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        User saved = userService.registerUser(
                request.getName(),
                request.getEmail(),
                request.getPassword(),
                request.getLicenseNumber()
        );
        // Return user info (password is @JsonIgnored in User entity)
        return ResponseEntity.ok(Map.of(
                "message", "Registration successful! Please log in.",
                "userId",  saved.getId(),
                "name",    saved.getName(),
                "role",    saved.getRole().name()
        ));
    }

    /**
     * Authenticate a user (admin or customer).
     * Returns a JWT token along with role and userId for client-side routing.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        User user = userService.authenticateUser(request.getEmail(), request.getPassword());
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return ResponseEntity.ok(new AuthResponse(token, user.getRole().name(), user.getId()));
    }
}
