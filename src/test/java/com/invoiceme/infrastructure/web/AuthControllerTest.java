package com.invoiceme.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceme.application.common.UserContext;
import com.invoiceme.application.user.dto.CreateUserRequest;
import com.invoiceme.application.user.dto.LoginRequest;
import com.invoiceme.domain.user.User;
import com.invoiceme.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void testSignup_Success() throws Exception {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setFirstName("New");
        request.setLastName("User");

        // When/Then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.id").exists())
                .andExpect(jsonPath("$.data.user.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.data.user.firstName").value("New"))
                .andExpect(jsonPath("$.data.user.lastName").value("User"));
    }

    @Test
    void testSignup_DuplicateEmail() throws Exception {
        // Given - create existing user
        User existing = new User();
        existing.create("existing@example.com", passwordEncoder.encode("password"), "Existing", "User");
        UserContext.setCurrentUser(UUID.randomUUID());
        userRepository.save(existing);
        UserContext.clear();

        // When - try to signup with same email
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("existing@example.com");
        request.setPassword("password123");
        request.setFirstName("New");
        request.setLastName("User");

        // Then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    void testSignup_ValidationErrors_EmptyEmail() throws Exception {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("");
        request.setPassword("password123");
        request.setFirstName("Test");
        request.setLastName("User");

        // When/Then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testSignup_ValidationErrors_InvalidEmail() throws Exception {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("invalid-email");
        request.setPassword("password123");
        request.setFirstName("Test");
        request.setLastName("User");

        // When/Then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testSignup_ValidationErrors_ShortPassword() throws Exception {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("test@example.com");
        request.setPassword("short");
        request.setFirstName("Test");
        request.setLastName("User");

        // When/Then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testSignup_ValidationErrors_MissingFields() throws Exception {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        // Missing firstName and lastName

        // When/Then
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testLogin_Success() throws Exception {
        // Given - create user
        User user = new User();
        String hashedPassword = passwordEncoder.encode("password123");
        user.create("login@example.com", hashedPassword, "Login", "User");
        UserContext.setCurrentUser(UUID.randomUUID());
        userRepository.save(user);
        UserContext.clear();

        // When - login
        LoginRequest request = new LoginRequest();
        request.setEmail("login@example.com");
        request.setPassword("password123");

        // Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("login@example.com"))
                .andExpect(jsonPath("$.data.user.firstName").value("Login"))
                .andExpect(jsonPath("$.data.user.lastName").value("User"));
    }

    @Test
    void testLogin_InvalidPassword() throws Exception {
        // Given - create user
        User user = new User();
        user.create("invalidpass@example.com", passwordEncoder.encode("correctpassword"), "Test", "User");
        UserContext.setCurrentUser(UUID.randomUUID());
        userRepository.save(user);
        entityManager.flush();
        UserContext.clear();

        // When - try with wrong password
        LoginRequest request = new LoginRequest();
        request.setEmail("invalidpass@example.com");
        request.setPassword("wrongpassword");

        // Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void testLogin_UserNotFound() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void testLogin_ValidationErrors_EmptyEmail() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword("password123");

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testLogin_ValidationErrors_EmptyPassword() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("");

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testLogin_DoesNotReturnPasswordHash() throws Exception {
        // Given - create user
        User user = new User();
        String hashedPassword = passwordEncoder.encode("password123");
        user.create("secure@example.com", hashedPassword, "Secure", "User");
        UserContext.setCurrentUser(UUID.randomUUID());
        userRepository.save(user);
        UserContext.clear();

        // When - login
        LoginRequest request = new LoginRequest();
        request.setEmail("secure@example.com");
        request.setPassword("password123");

        // Then - response should not contain password or passwordHash
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify password hash is not in response
        assert !response.contains("password");
        assert !response.contains("passwordHash");
        assert !response.contains(hashedPassword);
    }
}
