package com.ecommerce.paymentservice.config;

// Spring Security imports
import com.ecommerce.paymentservice.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security Configuration
 *
 * Configures Spring Security for JWT-based authentication
 *
 * Key Configurations:
 * 1. Password encoding (BCrypt)
 * 2. Authentication provider
 * 3. Public vs secured endpoints
 * 4. JWT filter
 * 5. Stateless session management
 */
@Configuration // Spring configuration class
@EnableWebSecurity // Enable Spring Security
@EnableMethodSecurity // Enable @PreAuthorize, @PostAuthorize annotations
public class SecurityConfig {

    @Autowired
    private UserDetailsService userDetailsService; // Load users

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter; // JWT filter

    /**
     * Password Encoder Bean
     *
     * BCrypt password encoder for hashing passwords
     *
     * Why BCrypt?
     * - Slow by design (prevents brute force)
     * - Automatic salt generation
     * - Adaptive (can increase rounds)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // BCrypt with default strength (10 rounds)
    }

    /**
     * Authentication Provider
     *
     * Configures how to authenticate users
     *
     * Uses:
     * - UserDetailsService to load user
     * - PasswordEncoder to compare passwords
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(userDetailsService); // Load user
        authProvider.setPasswordEncoder(passwordEncoder()); // Compare passwords

        return authProvider;
    }

    /**
     * Authentication Manager
     *
     * Used to authenticate users programmatically
     * Used in AuthController for login
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig
    ) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Security Filter Chain
     *
     * Configures HTTP security
     *
     * @param http HttpSecurity object
     * @return SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF (not needed for stateless JWT)
                .csrf(csrf -> csrf.disable())

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no authentication required)
                        .requestMatchers(
                                "/api/auth/**", // Login, register
                                "/actuator/**", // Health checks
                                "/swagger-ui/**", // Swagger UI
                                "/v3/api-docs/**" // OpenAPI docs
                        ).permitAll()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )

                // Stateless session management (no sessions, only JWT)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Set authentication provider
                .authenticationProvider(authenticationProvider())

                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}
