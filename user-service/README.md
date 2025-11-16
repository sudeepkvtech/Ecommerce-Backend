# User Service - E-commerce Microservices

Comprehensive microservice for user authentication, authorization, and profile management using Spring Boot 3.2, Spring Security, JWT, MySQL, and Netflix Eureka.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [JWT Authentication](#jwt-authentication)
- [Spring Security](#spring-security)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Database Schema](#database-schema)
- [Configuration](#configuration)

---

## 🎯 Overview

The User Service handles all user-related operations including:
- User registration with email verification
- User authentication (login) with JWT token generation
- Role-based authorization (ROLE_USER, ROLE_ADMIN)
- User profile management
- Address management (shipping/billing)
- Password management

---

## ✨ Features

**Authentication & Security:**
- ✅ JWT (JSON Web Token) based stateless authentication
- ✅ BCrypt password hashing (never store plain text!)
- ✅ Role-based access control (RBAC)
- ✅ Account activation/deactivation
- ✅ Secure endpoints with method-level security

**User Management:**
- ✅ User registration with validation
- ✅ User login with JWT token generation
- ✅ User profile updates
- ✅ Multiple addresses per user (shipping/billing)
- ✅ Default address for quick checkout

**Service Discovery:**
- ✅ Eureka client integration
- ✅ Automatic service registration
- ✅ Health monitoring
- ✅ Load balancing support

---

## 🛠️ Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Programming language |
| Spring Boot | 3.2.0 | Application framework |
| Spring Security | 3.2.0 | Authentication & authorization |
| Spring Data JPA | 3.2.0 | Database operations |
| JJWT | 0.12.3 | JWT token generation/validation |
| MySQL | 8.0 | Relational database |
| Netflix Eureka | 4.x | Service discovery |
| BCrypt | Built-in | Password hashing |
| Maven | 3.9+ | Build tool |
| Docker | Latest | Containerization |

---

## 🏗️ Architecture

### Layered Architecture
```
Client → Controller → Service → Repository → Database
           ↓            ↓
      JWT Filter   BCrypt/JWT
```

### Authentication Flow
```
1. User Registration:
   Client → POST /api/auth/register → AuthController → AuthService
   → Hash password (BCrypt) → Save to DB → Return UserResponse

2. User Login:
   Client → POST /api/auth/login → AuthController → AuthService
   → Authenticate (Spring Security) → Generate JWT → Return token

3. Authenticated Request:
   Client → GET /api/users/profile (with JWT in header)
   → JwtAuthenticationFilter → Validate JWT → Extract user
   → Set Security Context → UserController → Return data
```

---

## 🔐 JWT Authentication

### What is JWT?

JWT (JSON Web Token) is a compact, URL-safe token format for securely transmitting information between parties.

**Structure:** `header.payload.signature`

**Example JWT:**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGV4YW1wbGUuY29tIiwiZXhwIjoxNjQwOTk1MjAwfQ.signature
```

**Decoded:**
```json
// Header
{
  "alg": "HS256",
  "typ": "JWT"
}

// Payload
{
  "sub": "user@example.com",
  "exp": 1640995200
}

// Signature (verified with secret key)
```

### Why JWT?

**Advantages:**
- **Stateless**: Server doesn't store sessions
- **Scalable**: Works across multiple servers
- **Self-contained**: Token contains all user info
- **Secure**: Digitally signed (prevents tampering)
- **Mobile-friendly**: Works with mobile apps, SPAs

**Disadvantages:**
- **Cannot revoke**: Token valid until expiration
- **Size**: Larger than session IDs
- **Security**: Requires HTTPS (tokens in headers)

### JWT Implementation in User Service

**1. Token Generation (Login):**
```java
// In AuthService.java
public JwtResponse login(LoginRequest request) {
    // Authenticate user
    Authentication auth = authenticationManager.authenticate(...);

    // Generate JWT token
    String token = jwtUtil.generateToken(auth);

    // Return token to client
    return JwtResponse.builder()
        .token(token)
        .type("Bearer")
        .build();
}
```

**2. Token Validation (Every Request):**
```java
// In JwtAuthenticationFilter.java
protected void doFilterInternal(...) {
    // Extract token from Authorization header
    String jwt = getJwtFromRequest(request);

    // Validate token
    if (jwt != null && jwtUtil.validateToken(jwt)) {
        // Extract username
        String username = jwtUtil.getUsernameFromToken(jwt);

        // Load user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // Set authentication in Security Context
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
```

**3. Token Usage (Client):**
```javascript
// 1. Login and store token
const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
});
const { token } = await response.json();
localStorage.setItem('token', token);

// 2. Use token in subsequent requests
const profile = await fetch('/api/users/profile', {
    headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`
    }
});
```

### JWT Configuration

**application.yml:**
```yaml
jwt:
  secret: YourSecretKeyHere  # Change in production!
  expiration: 86400000        # 24 hours in milliseconds
  token-prefix: "Bearer "
  header-name: Authorization
```

**Security Best Practices:**
1. ✅ Use strong, random secret (min 256 bits)
2. ✅ Store secret in environment variables
3. ✅ Use HTTPS in production
4. ✅ Set appropriate expiration time
5. ✅ Implement refresh tokens for long sessions
6. ✅ Validate token on every request
7. ✅ Log security events

---

## 🛡️ Spring Security

### What is Spring Security?

Spring Security is a powerful framework that provides:
- Authentication (who you are)
- Authorization (what you can do)
- Protection against common attacks (CSRF, XSS, etc.)

### Security Configuration

**SecurityConfig.java:**
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            // Public endpoints (no authentication)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            // Stateless (no sessions, use JWT)
            .sessionManagement(session ->
                session.sessionCreationPolicy(STATELESS)
            )
            // Add JWT filter
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

### Password Security

**BCrypt Hashing:**
```java
// Registration
String hashedPassword = passwordEncoder.encode("password123");
// Stored: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

// Login
boolean matches = passwordEncoder.matches("password123", hashedPassword);
// Returns: true
```

**Why BCrypt?**
- Slow by design (prevents brute force)
- Automatic salt generation
- Adaptive (can increase rounds)
- Industry standard

### Role-Based Access Control

**Roles:**
- `ROLE_USER`: Regular customers
- `ROLE_ADMIN`: Administrators

**Usage in Controllers:**
```java
// Only admins can access
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/users/{id}")
public void deleteUser(@PathVariable Long id) {
    userService.deleteUser(id);
}

// User can only access their own data
@PreAuthorize("#id == authentication.principal.id or hasRole('ADMIN')")
@PutMapping("/users/{id}")
public UserResponse updateUser(@PathVariable Long id, @RequestBody UserRequest request) {
    return userService.updateUser(id, request);
}
```

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.9+
- MySQL 8.0+
- Docker (optional)
- Eureka Server running on port 8761

### 1. Setup Database

**Using Docker (Recommended):**
```bash
cd user-service
docker-compose up -d
```

**Manual MySQL Setup:**
```sql
CREATE DATABASE userdb;
```

### 2. Build and Run

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run
```

Service starts on: **http://localhost:8082**

### 3. Initialize Roles

**Important:** Create roles before registering users:

```sql
INSERT INTO roles (name) VALUES ('ROLE_USER');
INSERT INTO roles (name) VALUES ('ROLE_ADMIN');
```

### 4. Test API

**Register User:**
```bash
curl -X POST http://localhost:8082/user-service/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john@example.com",
    "password": "password123"
  }'
```

**Login:**
```bash
curl -X POST http://localhost:8082/user-service/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "id": 1,
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["ROLE_USER"]
}
```

**Use Token:**
```bash
curl http://localhost:8082/user-service/api/users/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 📚 API Documentation

### Base URL
```
http://localhost:8082/user-service/api
```

### Authentication Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/auth/register` | Register new user | No |
| POST | `/auth/login` | Login (get JWT token) | No |

### User Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/users/profile` | Get current user profile | Yes |
| PUT | `/users/profile` | Update current user | Yes |
| GET | `/users/{id}` | Get user by ID | Yes (Admin only) |

### Address Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/users/addresses` | Add address | Yes |
| GET | `/users/addresses` | Get user addresses | Yes |
| PUT | `/users/addresses/{id}` | Update address | Yes |
| DELETE | `/users/addresses/{id}` | Delete address | Yes |

---

## 💾 Database Schema

### Users Table
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- BCrypt hash
    phone VARCHAR(20),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_email (email)
);
```

### Roles Table
```sql
CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE
);
```

### User_Roles Table
```sql
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);
```

### Addresses Table
```sql
CREATE TABLE addresses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    street_address VARCHAR(500) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    address_type VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_user_id (user_id)
);
```

---

## ⚙️ Configuration

### application.yml

```yaml
server:
  port: 8082

spring:
  application:
    name: user-service
  datasource:
    url: jdbc:mysql://localhost:3307/userdb
    username: root
    password: root

jwt:
  secret: ChangeThisInProduction
  expiration: 86400000  # 24 hours

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### Environment Variables

Override configuration with environment variables:

```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/userdb
export SPRING_DATASOURCE_PASSWORD=SecurePassword
export JWT_SECRET=VeryLongRandomSecretKey
export EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka:8761/eureka/
```

---

## 🔒 Security Best Practices

1. **JWT Secret**: Use strong, random key (256+ bits)
2. **HTTPS**: Always use HTTPS in production
3. **Token Expiration**: Set reasonable expiration (15-60 minutes)
4. **Refresh Tokens**: Implement for long sessions
5. **Password Policy**: Enforce strong passwords
6. **Rate Limiting**: Prevent brute force attacks
7. **Email Verification**: Verify email on registration
8. **2FA**: Implement two-factor authentication
9. **Audit Logs**: Log all authentication events
10. **Regular Updates**: Keep dependencies updated

---

## 📝 License

Part of E-commerce Microservices learning project.

---

## 📧 Contact

For questions or issues, please create an issue in the repository.
