# Phase 2: Authentication (Full Stack)

## Overview
This phase implements complete authentication functionality including user management, JWT tokens, and login/signup screens. This is a full vertical slice - backend API, frontend UI, and integration tests.

**Goal:** Users can sign up, log in, and access protected routes with JWT authentication.

**Duration Estimate:** 1-2 days

---

## User Stories

### US-9: Create User entity, repository, and database migration (Backend)
**As a developer, I need User entity and repository so that users can be stored in the database**

**Acceptance Criteria:**
- User entity extends BaseEntity
- Fields: email (unique), passwordHash, firstName, lastName
- UserRepository with email lookup methods
- Flyway migration creates users table
- Database schema includes all BaseEntity fields

**Implementation Details:**

1. Create database migration `src/main/resources/db/migration/V1__create_users_table.sql`:

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,

    -- Audit fields
    created_at TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    last_modified_by UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    -- Soft delete
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID
);

-- Unique email for non-deleted users
CREATE UNIQUE INDEX idx_users_email_active
    ON users(email)
    WHERE is_deleted = FALSE;

-- Index for authentication lookups
CREATE INDEX idx_users_email
    ON users(email)
    WHERE is_deleted = FALSE;
```

2. Create `src/main/java/com/invoiceme/domain/user/User.java`:

```java
package com.invoiceme.domain.user;

import com.invoiceme.domain.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    // === Constructor ===

    public User() {
    }

    // === Business Methods ===

    public void create(String email, String passwordHash, String firstName, String lastName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // === Getters and Setters ===

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
```

3. Create `src/main/java/com/invoiceme/infrastructure/persistence/UserRepository.java`:

```java
package com.invoiceme.infrastructure.persistence;

import com.invoiceme.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailAndIsDeletedFalse(String email);
    boolean existsByEmailAndIsDeletedFalse(String email);
}
```

**Testing:**
```java
@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testCreateUser() {
        User user = new User();
        user.create("test@example.com", "hashedPassword", "John", "Doe");

        // Set UserContext manually for test
        UserContext.setCurrentUser(UUID.randomUUID());

        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertEquals("test@example.com", saved.getEmail());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getCreatedBy());

        UserContext.clear();
    }

    @Test
    void testFindByEmail() {
        // Create and save user
        User user = new User();
        user.create("find@example.com", "hash", "Jane", "Doe");
        UserContext.setCurrentUser(UUID.randomUUID());
        userRepository.save(user);

        // Find by email
        Optional<User> found = userRepository.findByEmailAndIsDeletedFalse("find@example.com");
        assertTrue(found.isPresent());
        assertEquals("Jane", found.get().getFirstName());

        UserContext.clear();
    }

    @Test
    void testEmailUniqueness() {
        User user1 = new User();
        user1.create("unique@example.com", "hash1", "User", "One");
        UserContext.setCurrentUser(UUID.randomUUID());
        userRepository.save(user1);

        User user2 = new User();
        user2.create("unique@example.com", "hash2", "User", "Two");

        // Should throw exception due to unique constraint
        assertThrows(Exception.class, () -> userRepository.save(user2));

        UserContext.clear();
    }
}
```

---

### US-10: Implement JWT token provider and authentication filter (Backend)
**As a developer, I need JWT token provider so that users can be authenticated**

**Acceptance Criteria:**
- JwtTokenProvider generates and validates tokens
- Token includes userId and email claims
- Token expiration configurable
- JwtAuthenticationFilter extracts token and sets context
- UserContext populated on each request

**Implementation Details:**

1. Create `src/main/java/com/invoiceme/infrastructure/security/JwtTokenProvider.java`:

```java
package com.invoiceme.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    public String generateToken(UUID userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("email", email)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return UUID.fromString(claims.getSubject());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

2. Create `src/main/java/com/invoiceme/infrastructure/security/JwtAuthenticationFilter.java`:

```java
package com.invoiceme.infrastructure.security;

import com.invoiceme.application.common.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (jwt != null && tokenProvider.validateToken(jwt)) {
                UUID userId = tokenProvider.getUserIdFromToken(jwt);

                // Set in Spring Security context
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Set in UserContext for audit trail
                UserContext.setCurrentUser(userId);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

3. Create `src/main/java/com/invoiceme/infrastructure/security/SecurityConfig.java`:

```java
package com.invoiceme.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

**Testing:**
```java
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
            "test-secret-key-minimum-256-bits-required-for-hmac-sha",
            86400000L
        );
    }

    @Test
    void testGenerateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";

        String token = tokenProvider.generateToken(userId, email);
        assertNotNull(token);
        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    void testExtractUserIdFromToken() {
        UUID userId = UUID.randomUUID();
        String token = tokenProvider.generateToken(userId, "test@example.com");

        UUID extractedId = tokenProvider.getUserIdFromToken(token);
        assertEquals(userId, extractedId);
    }

    @Test
    void testInvalidTokenReturnsFalse() {
        assertFalse(tokenProvider.validateToken("invalid-token"));
    }
}
```

---

### US-11: Create signup endpoint (Backend)
**As a backend developer, I need signup endpoint so that users can create accounts**

**Acceptance Criteria:**
- POST /api/auth/signup endpoint
- Validates email uniqueness
- Hashes password with BCrypt
- Returns UserResponse + JWT token
- Handles validation errors

**Implementation Details:**

1. Create DTOs in `src/main/java/com/invoiceme/application/user/dto/`:

```java
package com.invoiceme.application.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    // Getters and setters
}
```

```java
package com.invoiceme.application.user.dto;

import java.util.UUID;

public class UserResponse {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;

    // Getters and setters
}
```

```java
package com.invoiceme.application.user.dto;

public class AuthResponse {

    private String token;
    private UserResponse user;

    public AuthResponse(String token, UserResponse user) {
        this.token = token;
        this.user = user;
    }

    // Getters and setters
}
```

2. Create `src/main/java/com/invoiceme/application/user/UserMapper.java`:

```java
package com.invoiceme.application.user;

import com.invoiceme.application.user.dto.UserResponse;
import com.invoiceme.domain.user.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        UserResponse dto = new UserResponse();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        return dto;
    }
}
```

3. Create `src/main/java/com/invoiceme/application/user/UserService.java`:

```java
package com.invoiceme.application.user;

import com.invoiceme.application.common.UserContext;
import com.invoiceme.application.user.dto.CreateUserRequest;
import com.invoiceme.application.user.dto.UserResponse;
import com.invoiceme.domain.common.exceptions.ValidationException;
import com.invoiceme.domain.user.User;
import com.invoiceme.infrastructure.persistence.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        // Check email uniqueness
        if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
            throw new ValidationException("User with email " + request.getEmail() + " already exists");
        }

        // Create user
        User user = new User();
        String passwordHash = passwordEncoder.encode(request.getPassword());
        user.create(request.getEmail(), passwordHash, request.getFirstName(), request.getLastName());

        // Set UserContext to user's own ID for audit trail
        // (This is a special case - normally UserContext is set by JWT filter)
        UserContext.setCurrentUser(user.getId());

        userRepository.save(user);

        return userMapper.toResponse(user);
    }
}
```

4. Create `src/main/java/com/invoiceme/infrastructure/web/AuthController.java`:

```java
package com.invoiceme.infrastructure.web;

import com.invoiceme.application.common.ApiResponse;
import com.invoiceme.application.user.UserService;
import com.invoiceme.application.user.dto.AuthResponse;
import com.invoiceme.application.user.dto.CreateUserRequest;
import com.invoiceme.application.user.dto.UserResponse;
import com.invoiceme.infrastructure.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = userService.createUser(request);
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        AuthResponse authResponse = new AuthResponse(token, user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", authResponse));
    }
}
```

**Testing:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerSignupTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSignupSuccess() throws Exception {
        String requestBody = """
            {
                "email": "newuser@example.com",
                "password": "password123",
                "firstName": "New",
                "lastName": "User"
            }
            """;

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.user.email").value("newuser@example.com"));
    }

    @Test
    void testSignupDuplicateEmail() throws Exception {
        // Create user first
        User existing = new User();
        existing.create("existing@example.com", "hash", "Existing", "User");
        UserContext.setCurrentUser(UUID.randomUUID());
        userRepository.save(existing);
        UserContext.clear();

        // Try to signup with same email
        String requestBody = """
            {
                "email": "existing@example.com",
                "password": "password123",
                "firstName": "New",
                "lastName": "User"
            }
            """;

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    void testSignupValidationErrors() throws Exception {
        String requestBody = """
            {
                "email": "invalid-email",
                "password": "short",
                "firstName": "",
                "lastName": ""
            }
            """;

        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
```

---

### US-12: Create login endpoint (Backend)
**As a backend developer, I need login endpoint so that users can authenticate**

**Acceptance Criteria:**
- POST /api/auth/login endpoint
- Validates credentials
- Returns UserResponse + JWT token
- Returns 401 for invalid credentials

**Implementation Details:**

1. Create `src/main/java/com/invoiceme/application/user/dto/LoginRequest.java`:

```java
package com.invoiceme.application.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    // Getters and setters
}
```

2. Add to `UserService.java`:

```java
public UserResponse authenticate(LoginRequest request) {
    User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
            .orElseThrow(() -> new ValidationException("Invalid email or password"));

    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
        throw new ValidationException("Invalid email or password");
    }

    return userMapper.toResponse(user);
}
```

3. Add to `AuthController.java`:

```java
@PostMapping("/login")
public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
    UserResponse user = userService.authenticate(request);
    String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
    AuthResponse authResponse = new AuthResponse(token, user);

    return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
}
```

**Testing:**
```java
@Test
void testLoginSuccess() throws Exception {
    // Create user
    User user = new User();
    String hashedPassword = passwordEncoder.encode("password123");
    user.create("login@example.com", hashedPassword, "Login", "User");
    UserContext.setCurrentUser(UUID.randomUUID());
    userRepository.save(user);
    UserContext.clear();

    // Login
    String requestBody = """
        {
            "email": "login@example.com",
            "password": "password123"
        }
        """;

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.token").exists())
            .andExpect(jsonPath("$.data.user.email").value("login@example.com"));
}

@Test
void testLoginInvalidPassword() throws Exception {
    // Create user
    User user = new User();
    user.create("test@example.com", passwordEncoder.encode("correctpassword"), "Test", "User");
    UserContext.setCurrentUser(UUID.randomUUID());
    userRepository.save(user);
    UserContext.clear();

    // Try with wrong password
    String requestBody = """
        {
            "email": "test@example.com",
            "password": "wrongpassword"
        }
        """;

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
}

@Test
void testLoginUserNotFound() throws Exception {
    String requestBody = """
        {
            "email": "nonexistent@example.com",
            "password": "password123"
        }
        """;

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
}
```

---

### US-13: Setup frontend API client with interceptors (Frontend)
**As a frontend developer, I need API client configured so that I can call backend endpoints**

**Acceptance Criteria:**
- Axios client with base URL configuration
- Request interceptor adds JWT token
- Response interceptor handles 401 errors
- Environment variable for API URL

**Implementation Details:**

1. Install dependencies:
```bash
npm install axios @tanstack/react-query react-router-dom
```

2. Create `.env.local`:
```
VITE_API_BASE_URL=http://localhost:8080/api
```

3. Create `src/api/client.ts`:

```typescript
import axios, { AxiosError } from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor: Add JWT token
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor: Handle common errors
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      // Unauthorized - clear auth and redirect to login
      localStorage.removeItem('auth_token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
```

4. Create `src/models/ApiResponse.ts`:

```typescript
export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
}
```

5. Create `src/models/User.ts`:

```typescript
export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface CreateUserRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}
```

6. Create `src/api/authApi.ts`:

```typescript
import { apiClient } from './client';
import { ApiResponse } from '../models/ApiResponse';
import { LoginRequest, CreateUserRequest, AuthResponse } from '../models/User';

export const authApi = {
  login: async (request: LoginRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', request);
    return response.data.data;
  },

  signup: async (request: CreateUserRequest): Promise<AuthResponse> => {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/auth/signup', request);
    return response.data.data;
  },
};
```

**Testing:**
- Manual test: Verify axios makes requests to http://localhost:8080/api
- Verify token is added to Authorization header when present
- Verify 401 redirects to login

---

### US-14: Create AuthContext and authentication hooks (Frontend)
**As a frontend developer, I need AuthContext so that I can manage authentication state**

**Acceptance Criteria:**
- AuthContext provides user, token, isAuthenticated
- login() and logout() functions
- localStorage persistence
- useAuth() hook for easy access

**Implementation Details:**

1. Create `src/context/AuthContext.tsx`:

```typescript
import React, { createContext, useState, useEffect, ReactNode } from 'react';
import { User } from '../models/User';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (token: string, user: User) => void;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

interface AuthProviderProps {
  children: ReactNode;
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [token, setToken] = useState<string | null>(
    () => localStorage.getItem('auth_token')
  );
  const [user, setUser] = useState<User | null>(
    () => {
      const stored = localStorage.getItem('user');
      return stored ? JSON.parse(stored) : null;
    }
  );

  const login = (newToken: string, newUser: User) => {
    setToken(newToken);
    setUser(newUser);
    localStorage.setItem('auth_token', newToken);
    localStorage.setItem('user', JSON.stringify(newUser));
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('auth_token');
    localStorage.removeItem('user');
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};
```

2. Create `src/hooks/useAuth.ts`:

```typescript
import { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
```

**Testing:**
- Verify login() stores token and user in localStorage
- Verify logout() clears localStorage
- Verify isAuthenticated is true when token exists
- Verify AuthContext values accessible via useAuth()

---

### US-15: Create signup screen (Frontend Full Stack)
**As a user, I need a signup screen so that I can create an account**

**Acceptance Criteria:**
- Form with email, password, firstName, lastName
- Inline validation
- Calls signup API
- Shows loading state during submission
- Navigates to /customers on success
- Shows error messages on failure

**Implementation Details:**

1. Create `src/viewmodels/auth/SignupViewModel.ts`:

```typescript
import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../../api/authApi';
import { CreateUserRequest } from '../../models/User';
import { useAuth } from '../../hooks/useAuth';

export const SignupViewModel = () => {
  const navigate = useNavigate();
  const { login } = useAuth();

  // Form state
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');

  // Validation errors
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Signup mutation
  const signupMutation = useMutation({
    mutationFn: authApi.signup,
    onSuccess: (response) => {
      login(response.token, response.user);
      navigate('/customers');
    },
    onError: (error: any) => {
      if (error.response?.data?.message) {
        setErrors({ submit: error.response.data.message });
      } else {
        setErrors({ submit: 'Failed to create account' });
      }
    },
  });

  // Validation
  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!email.trim()) {
      newErrors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      newErrors.email = 'Must be a valid email address';
    }

    if (!password) {
      newErrors.password = 'Password is required';
    } else if (password.length < 8) {
      newErrors.password = 'Password must be at least 8 characters';
    }

    if (!firstName.trim()) {
      newErrors.firstName = 'First name is required';
    }

    if (!lastName.trim()) {
      newErrors.lastName = 'Last name is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Submit
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!validate()) return;

    const request: CreateUserRequest = {
      email,
      password,
      firstName,
      lastName,
    };

    signupMutation.mutate(request);
  };

  return {
    email,
    setEmail,
    password,
    setPassword,
    firstName,
    setFirstName,
    lastName,
    setLastName,
    errors,
    handleSubmit,
    isSubmitting: signupMutation.isPending,
  };
};
```

2. Create `src/views/auth/SignupView.tsx`:

```typescript
import React from 'react';
import { Link } from 'react-router-dom';
import { SignupViewModel } from '../../viewmodels/auth/SignupViewModel';

export const SignupView: React.FC = () => {
  const vm = SignupViewModel();

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-6">
      <div className="max-w-md w-full bg-white rounded-lg shadow-md p-8">
        <h1 className="text-3xl font-bold text-center mb-6">Create Account</h1>

        <form onSubmit={vm.handleSubmit} className="space-y-4">
          {/* Email */}
          <div>
            <label className="block text-sm font-medium mb-2">Email</label>
            <input
              type="email"
              value={vm.email}
              onChange={(e) => vm.setEmail(e.target.value)}
              className={`w-full border rounded px-3 py-2 ${
                vm.errors.email ? 'border-red-500' : 'border-gray-300'
              }`}
            />
            {vm.errors.email && (
              <p className="text-red-500 text-sm mt-1">{vm.errors.email}</p>
            )}
          </div>

          {/* Password */}
          <div>
            <label className="block text-sm font-medium mb-2">Password</label>
            <input
              type="password"
              value={vm.password}
              onChange={(e) => vm.setPassword(e.target.value)}
              className={`w-full border rounded px-3 py-2 ${
                vm.errors.password ? 'border-red-500' : 'border-gray-300'
              }`}
            />
            {vm.errors.password && (
              <p className="text-red-500 text-sm mt-1">{vm.errors.password}</p>
            )}
          </div>

          {/* First Name */}
          <div>
            <label className="block text-sm font-medium mb-2">First Name</label>
            <input
              type="text"
              value={vm.firstName}
              onChange={(e) => vm.setFirstName(e.target.value)}
              className={`w-full border rounded px-3 py-2 ${
                vm.errors.firstName ? 'border-red-500' : 'border-gray-300'
              }`}
            />
            {vm.errors.firstName && (
              <p className="text-red-500 text-sm mt-1">{vm.errors.firstName}</p>
            )}
          </div>

          {/* Last Name */}
          <div>
            <label className="block text-sm font-medium mb-2">Last Name</label>
            <input
              type="text"
              value={vm.lastName}
              onChange={(e) => vm.setLastName(e.target.value)}
              className={`w-full border rounded px-3 py-2 ${
                vm.errors.lastName ? 'border-red-500' : 'border-gray-300'
              }`}
            />
            {vm.errors.lastName && (
              <p className="text-red-500 text-sm mt-1">{vm.errors.lastName}</p>
            )}
          </div>

          {/* Submit Error */}
          {vm.errors.submit && (
            <div className="bg-red-50 border border-red-300 rounded p-3">
              <p className="text-red-700 text-sm">{vm.errors.submit}</p>
            </div>
          )}

          {/* Submit Button */}
          <button
            type="submit"
            disabled={vm.isSubmitting}
            className="w-full bg-blue-600 text-white px-6 py-3 rounded hover:bg-blue-700 disabled:opacity-50"
          >
            {vm.isSubmitting ? 'Creating Account...' : 'Sign Up'}
          </button>
        </form>

        <p className="text-center text-gray-600 mt-6">
          Already have an account?{' '}
          <Link to="/login" className="text-blue-600 hover:underline">
            Log in
          </Link>
        </p>
      </div>
    </div>
  );
};
```

**Testing:**
- Fill out form and submit
- Verify API call made to POST /api/auth/signup
- Verify token stored in localStorage
- Verify navigation to /customers
- Test validation errors display
- Test backend error messages display
- Test duplicate email error

---

### US-16: Create login screen (Frontend Full Stack)
**As a user, I need a login screen so that I can access the application**

**Acceptance Criteria:**
- Form with email and password
- Inline validation
- Calls login API
- Shows loading state during submission
- Navigates to /customers on success
- Shows error messages on failure

**Implementation Details:**

1. Create `src/viewmodels/auth/LoginViewModel.ts`:

```typescript
import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../../api/authApi';
import { LoginRequest } from '../../models/User';
import { useAuth } from '../../hooks/useAuth';

export const LoginViewModel = () => {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});

  const loginMutation = useMutation({
    mutationFn: authApi.login,
    onSuccess: (response) => {
      login(response.token, response.user);
      navigate('/customers');
    },
    onError: (error: any) => {
      if (error.response?.data?.message) {
        setErrors({ submit: error.response.data.message });
      } else {
        setErrors({ submit: 'Login failed' });
      }
    },
  });

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!email.trim()) {
      newErrors.email = 'Email is required';
    }

    if (!password) {
      newErrors.password = 'Password is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!validate()) return;

    const request: LoginRequest = { email, password };
    loginMutation.mutate(request);
  };

  return {
    email,
    setEmail,
    password,
    setPassword,
    errors,
    handleSubmit,
    isSubmitting: loginMutation.isPending,
  };
};
```

2. Create `src/views/auth/LoginView.tsx`:

```typescript
import React from 'react';
import { Link } from 'react-router-dom';
import { LoginViewModel } from '../../viewmodels/auth/LoginViewModel';

export const LoginView: React.FC = () => {
  const vm = LoginViewModel();

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center p-6">
      <div className="max-w-md w-full bg-white rounded-lg shadow-md p-8">
        <h1 className="text-3xl font-bold text-center mb-6">Log In</h1>

        <form onSubmit={vm.handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium mb-2">Email</label>
            <input
              type="email"
              value={vm.email}
              onChange={(e) => vm.setEmail(e.target.value)}
              className={`w-full border rounded px-3 py-2 ${
                vm.errors.email ? 'border-red-500' : 'border-gray-300'
              }`}
            />
            {vm.errors.email && (
              <p className="text-red-500 text-sm mt-1">{vm.errors.email}</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-medium mb-2">Password</label>
            <input
              type="password"
              value={vm.password}
              onChange={(e) => vm.setPassword(e.target.value)}
              className={`w-full border rounded px-3 py-2 ${
                vm.errors.password ? 'border-red-500' : 'border-gray-300'
              }`}
            />
            {vm.errors.password && (
              <p className="text-red-500 text-sm mt-1">{vm.errors.password}</p>
            )}
          </div>

          {vm.errors.submit && (
            <div className="bg-red-50 border border-red-300 rounded p-3">
              <p className="text-red-700 text-sm">{vm.errors.submit}</p>
            </div>
          )}

          <button
            type="submit"
            disabled={vm.isSubmitting}
            className="w-full bg-blue-600 text-white px-6 py-3 rounded hover:bg-blue-700 disabled:opacity-50"
          >
            {vm.isSubmitting ? 'Logging In...' : 'Log In'}
          </button>
        </form>

        <p className="text-center text-gray-600 mt-6">
          Don't have an account?{' '}
          <Link to="/signup" className="text-blue-600 hover:underline">
            Sign up
          </Link>
        </p>
      </div>
    </div>
  );
};
```

**Testing:**
- Login with valid credentials
- Verify API call made
- Verify token stored
- Verify navigation to /customers
- Test validation errors
- Test invalid credentials error

---

### US-17: Setup routing and protected routes (Frontend)
**As a user, I need protected routes so that unauthenticated users cannot access the app**

**Acceptance Criteria:**
- React Router configured
- Public routes: /login, /signup
- Protected routes require authentication
- Redirect to /login if not authenticated
- Logout button in nav

**Implementation Details:**

1. Update `src/main.tsx`:

```typescript
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import App from './App';
import './index.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 5,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  </React.StrictMode>
);
```

2. Create `src/components/ProtectedRoute.tsx`:

```typescript
import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export const ProtectedRoute: React.FC = () => {
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div>
      {/* Placeholder for PageLayout - will add in Phase 3 */}
      <Outlet />
    </div>
  );
};
```

3. Create temporary placeholder view `src/views/customers/CustomerListView.tsx`:

```typescript
import React from 'react';
import { useAuth } from '../../hooks/useAuth';

export const CustomerListView: React.FC = () => {
  const { user, logout } = useAuth();

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="container mx-auto">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold">Customers</h1>
          <button
            onClick={logout}
            className="bg-gray-300 text-gray-700 px-4 py-2 rounded hover:bg-gray-400"
          >
            Logout
          </button>
        </div>
        <p>Welcome, {user?.firstName}! Customer list coming in Phase 3.</p>
      </div>
    </div>
  );
};
```

4. Update `src/App.tsx`:

```typescript
import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LoginView } from './views/auth/LoginView';
import { SignupView } from './views/auth/SignupView';
import { CustomerListView } from './views/customers/CustomerListView';

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginView />} />
        <Route path="/signup" element={<SignupView />} />

        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<Navigate to="/customers" replace />} />
          <Route path="/customers" element={<CustomerListView />} />
        </Route>
      </Routes>
    </AuthProvider>
  );
}

export default App;
```

**Testing:**
- Visit / while logged out → redirects to /login
- Login → redirects to /customers
- Visit /customers while logged in → shows customer list
- Click logout → redirects to /login
- Try to visit /customers while logged out → redirects to /login

---

### US-18: Add CORS configuration (Backend)
**As a developer, I need CORS configuration so that frontend can call backend**

**Acceptance Criteria:**
- CORS allows localhost:5173 (Vite dev server)
- Allows all HTTP methods
- Allows Authorization header

**Implementation Details:**

Create `src/main/java/com/invoiceme/infrastructure/web/config/WebConfig.java`:

```java
package com.invoiceme.infrastructure.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

**Testing:**
- Frontend can make requests to backend
- No CORS errors in browser console

---

## Phase 2 Completion Checklist

**Backend:**
- [ ] User entity and repository implemented
- [ ] Users table created via Flyway
- [ ] JWT token provider working
- [ ] JWT authentication filter working
- [ ] Spring Security configured
- [ ] POST /api/auth/signup endpoint works
- [ ] POST /api/auth/login endpoint works
- [ ] CORS configuration allows frontend
- [ ] Integration tests for auth pass

**Frontend:**
- [ ] API client with interceptors configured
- [ ] AuthContext provides authentication state
- [ ] Signup screen functional
- [ ] Login screen functional
- [ ] Protected routes work
- [ ] Logout works
- [ ] Token stored in localStorage
- [ ] Manual end-to-end test: signup → login → see customer list → logout

**Integration:**
- [ ] Frontend can signup via backend
- [ ] Frontend can login via backend
- [ ] Protected routes redirect when not authenticated
- [ ] Token included in API requests
- [ ] 401 responses clear auth and redirect to login

## Next Phase

Phase 3: Customer CRUD (Vertical Slices - each feature built full stack with tests)
