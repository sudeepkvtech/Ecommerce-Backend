package com.ecommerce.userservice.security;

// Spring Security imports
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Entity and Repository imports
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.repository.UserRepository;

// Java imports
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom UserDetailsService Implementation
 *
 * Spring Security uses this to load user data during authentication
 *
 * When user logs in:
 * 1. Spring Security calls loadUserByUsername(email)
 * 2. We load User from database
 * 3. Convert to Spring Security UserDetails
 * 4. Spring Security compares passwords
 * 5. If match: Authentication successful
 */
@Service // Spring service bean
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository; // Access to users database

    /**
     * Load user by username (email)
     *
     * Called by Spring Security during login
     *
     * @param email User's email (used as username)
     * @return UserDetails object for Spring Security
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Load user from database
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email
                ));

        // Convert roles to granted authorities
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet());

        // Build Spring Security UserDetails object
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail()) // Username is email
                .password(user.getPassword()) // BCrypt hashed password
                .authorities(authorities) // User's roles
                .accountExpired(false) // Account not expired
                .accountLocked(false) // Account not locked
                .credentialsExpired(false) // Password not expired
                .disabled(!user.getEnabled()) // Disabled if user.enabled = false
                .build();
    }
}
