package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.*;
import com.ecommerce.userservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller
 * Handles user registration and login
 *
 * Endpoints:
 * POST /api/auth/register - Register new user
 * POST /api/auth/login - Login user (get JWT token)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Register new user
     *
     * POST /api/auth/register
     * Body: { "firstName": "John", "lastName": "Doe", "email": "john@example.com", "password": "password123" }
     *
     * Returns: UserResponse with user details (no password)
     * Status: 201 Created
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login user
     *
     * POST /api/auth/login
     * Body: { "email": "john@example.com", "password": "password123" }
     *
     * Returns: JwtResponse with JWT token and user details
     * Status: 200 OK
     *
     * Client should:
     * 1. Store token (localStorage, sessionStorage, or cookie)
     * 2. Send token in Authorization header for subsequent requests:
     *    Authorization: Bearer <token>
     */
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        JwtResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
