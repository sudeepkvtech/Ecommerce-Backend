package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.*;
import com.ecommerce.userservice.entity.Role;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.repository.RoleRepository;
import com.ecommerce.userservice.repository.UserRepository;
import com.ecommerce.userservice.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authentication Service
 * Handles user registration and login
 */
@Service
@Transactional
public class AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtUtil jwtUtil;

    /**
     * Register new user
     * 1. Check if email already exists
     * 2. Create user with hashed password
     * 3. Assign ROLE_USER
     * 4. Save to database
     */
    public UserResponse register(RegisterRequest request) {
        // Check if email already registered
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Create new user
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail().toLowerCase()); // Store lowercase
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Hash password
        user.setPhone(request.getPhone());
        user.setEnabled(true); // Active by default

        // Assign ROLE_USER
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Role ROLE_USER not found"));
        user.setRoles(Set.of(userRole));

        // Save user
        User savedUser = userRepository.save(user);

        // Return response
        return toUserResponse(savedUser);
    }

    /**
     * Login user
     * 1. Authenticate with email/password
     * 2. Generate JWT token
     * 3. Return token and user info
     */
    public JwtResponse login(LoginRequest request) {
        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase(),
                        request.getPassword()
                )
        );

        // Generate JWT token
        String token = jwtUtil.generateToken(authentication);

        // Load user details
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Build response
        return JwtResponse.builder()
                .token(token)
                .type("Bearer")
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .build();
    }

    /**
     * Convert User entity to UserResponse DTO
     */
    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .enabled(user.getEnabled())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
