package com.vehiclerental.service;

import com.vehiclerental.model.Role;
import com.vehiclerental.model.User;
import com.vehiclerental.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public User registerUser(String name, String email, String password, String licenseNumber) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email '" + email + "' is already registered.");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setLicenseNumber(licenseNumber);
        user.setRole(Role.CUSTOMER);
        return userRepository.save(user);
    }

    public User authenticateUser(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("No account found with email: " + email);
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password. Please try again.");
        }

        return user;
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User with ID " + id + " not found."));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
