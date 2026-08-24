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

import java.util.List;
import java.util.Map;

@RestController
public class AuthController {

    @Autowired 
    private UserService userService;
    
    @Autowired 
    private JwtUtil jwtUtil;

    @PostMapping("/api/auth/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        User saved = userService.registerUser(
                request.getName(),
                request.getEmail(),
                request.getPassword(),
                request.getLicenseNumber()
        );
        return ResponseEntity.ok(Map.of(
                "message", "Registration successful! Please log in.",
                "userId",  saved.getId(),
                "name",    saved.getName(),
                "role",    saved.getRole().name()
        ));
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        User user = userService.authenticateUser(request.getEmail(), request.getPassword());
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId(), user.getName());
        return ResponseEntity.ok(new AuthResponse(token, user.getRole().name(), user.getId(), user.getName()));
    }

    @GetMapping("/api/admin/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // Service-to-Service internal API
    @GetMapping("/api/auth/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
}
