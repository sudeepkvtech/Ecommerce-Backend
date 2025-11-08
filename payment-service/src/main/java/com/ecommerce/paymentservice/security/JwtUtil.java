package com.ecommerce.paymentservice.security;

// JWT library imports
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
// Spring imports
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

// Java imports
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Utility Class
 * Handles JWT token generation, validation, and parsing
 *
 * What is JWT?
 * JWT (JSON Web Token) is a compact, URL-safe token format
 * Used for stateless authentication
 *
 * JWT Structure:
 * header.payload.signature
 * Example: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIn0.signature
 *
 * Why JWT?
 * - Stateless: Server doesn't store sessions
 * - Scalable: Works across multiple servers
 * - Self-contained: Token contains all user info
 * - Secure: Digitally signed to prevent tampering
 */
@Component // Spring bean - can be injected
public class JwtUtil {

    // JWT secret key from application.yml
    // Used to sign and verify tokens
    // MUST be kept secret! Change in production!
    @Value("${jwt.secret}")
    private String jwtSecret;

    // Token expiration time in milliseconds from application.yml
    // Default: 86400000 ms = 24 hours
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Generate JWT token from Authentication
     *
     * @param authentication Spring Security Authentication object
     * @return JWT token string
     *
     * Process:
     * 1. Extract username (email) from authentication
     * 2. Set subject (username)
     * 3. Set issued date (now)
     * 4. Set expiration date (now + expiration time)
     * 5. Sign with secret key
     * 6. Return token string
     */
    public String generateToken(Authentication authentication) {
        // Get authenticated user's username (email)
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();

        // Build and return JWT token
        return Jwts.builder()
                .setSubject(userPrincipal.getUsername()) // Set subject (username/email)
                .setIssuedAt(new Date()) // Set issued date to now
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration)) // Set expiration
                .signWith(getSigningKey()) // Sign with secret key
                .compact(); // Build token string
    }

    /**
     * Generate JWT token from username
     *
     * @param username User's email
     * @return JWT token string
     */
    public String generateTokenFromUsername(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract username from JWT token
     *
     * @param token JWT token string
     * @return username (email)
     *
     * Process:
     * 1. Parse token with signing key
     * 2. Extract claims (payload)
     * 3. Get subject (username)
     */
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // Set signing key for verification
                .build()
                .parseClaimsJws(token) // Parse and verify token
                .getBody() // Get claims (payload)
                .getSubject(); // Get subject (username)
    }

    /**
     * Validate JWT token
     *
     * @param token JWT token string
     * @return true if valid, false if invalid
     *
     * Validates:
     * 1. Signature is correct (not tampered)
     * 2. Token is not expired
     * 3. Token format is valid
     */
    public boolean validateToken(String token) {
        try {
            // Parse token - throws exception if invalid
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true; // Token is valid
        } catch (SecurityException ex) {
            // Invalid JWT signature
            System.err.println("Invalid JWT signature: " + ex.getMessage());
        } catch (MalformedJwtException ex) {
            // Invalid JWT token format
            System.err.println("Invalid JWT token: " + ex.getMessage());
        } catch (ExpiredJwtException ex) {
            // Token has expired
            System.err.println("JWT token is expired: " + ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            // JWT token is unsupported
            System.err.println("JWT token is unsupported: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            // JWT claims string is empty
            System.err.println("JWT claims string is empty: " + ex.getMessage());
        }
        return false; // Token is invalid
    }

    /**
     * Get signing key from secret
     *
     * @return SecretKey for signing/verifying tokens
     *
     * Converts secret string to SecretKey object
     * Uses HMAC SHA algorithm
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
