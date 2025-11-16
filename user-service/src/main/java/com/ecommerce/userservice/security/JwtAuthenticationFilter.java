package com.ecommerce.userservice.security;

// Spring Security imports
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Servlet imports
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * JWT Authentication Filter
 *
 * This filter intercepts EVERY HTTP request
 * Extracts and validates JWT token from Authorization header
 * Authenticates user if token is valid
 *
 * Filter Chain:
 * Request → JwtAuthenticationFilter → Spring Security → Controller
 *
 * Process:
 * 1. Extract token from Authorization header
 * 2. Validate token
 * 3. Load user details
 * 4. Set authentication in Security Context
 * 5. Continue filter chain
 */
@Component // Spring bean
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil; // JWT utility for token operations

    @Autowired
    private UserDetailsService userDetailsService; // Load user from database

    /**
     * Filter method - called for every request
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain Filter chain to continue
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Step 1: Extract JWT token from request
            String jwt = getJwtFromRequest(request);

            // Step 2: Validate token and authenticate
            if (jwt != null && jwtUtil.validateToken(jwt)) {
                // Token is valid, extract username
                String username = jwtUtil.getUsernameFromToken(jwt);

                // Load user details from database
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Create authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, // Principal (user)
                                null, // Credentials (not needed, already authenticated)
                                userDetails.getAuthorities() // Roles/authorities
                        );

                // Set request details
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Set authentication in Security Context
                // This tells Spring Security that user is authenticated
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // Log error but don't block request
            System.err.println("Cannot set user authentication: " + ex.getMessage());
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header
     *
     * @param request HTTP request
     * @return JWT token string or null
     *
     * Authorization header format:
     * "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     *
     * Extracts token by removing "Bearer " prefix
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        // Check if Authorization header exists and starts with "Bearer "
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            // Remove "Bearer " prefix (7 characters)
            return bearerToken.substring(7);
        }

        return null; // No token found
    }
}
