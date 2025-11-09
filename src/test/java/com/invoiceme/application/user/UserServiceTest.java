package com.invoiceme.application.user;

import com.invoiceme.application.common.UserContext;
import com.invoiceme.application.user.dto.CreateUserRequest;
import com.invoiceme.application.user.dto.LoginRequest;
import com.invoiceme.application.user.dto.UserResponse;
import com.invoiceme.domain.common.exceptions.ValidationException;
import com.invoiceme.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void testCreateUser_Success() {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("service-test@example.com");
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");

        // When
        UserResponse result = userService.createUser(request);

        // Then
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("service-test@example.com", result.getEmail());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());

        // Verify user was persisted
        assertTrue(userRepository.existsByEmailAndIsDeletedFalse("service-test@example.com"));
    }

    @Test
    void testCreateUser_EmailAlreadyExists() {
        // Given - create existing user first
        CreateUserRequest existingRequest = new CreateUserRequest();
        existingRequest.setEmail("service-existing@example.com");
        existingRequest.setPassword("password123");
        existingRequest.setFirstName("Existing");
        existingRequest.setLastName("User");
        userService.createUser(existingRequest);

        // When - try to create another user with same email
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("service-existing@example.com");
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");

        // Then
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.createUser(request);
        });

        assertEquals("User with email service-existing@example.com already exists", exception.getMessage());
    }

    @Test
    void testAuthenticate_Success() {
        // Given - create user first
        CreateUserRequest createRequest = new CreateUserRequest();
        createRequest.setEmail("service-auth@example.com");
        createRequest.setPassword("password123");
        createRequest.setFirstName("John");
        createRequest.setLastName("Doe");
        userService.createUser(createRequest);

        // When - authenticate
        LoginRequest request = new LoginRequest();
        request.setEmail("service-auth@example.com");
        request.setPassword("password123");

        UserResponse result = userService.authenticate(request);

        // Then
        assertNotNull(result);
        assertEquals("service-auth@example.com", result.getEmail());
        assertEquals("John", result.getFirstName());
    }

    @Test
    void testAuthenticate_UserNotFound() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("service-nonexistent@example.com");
        request.setPassword("password123");

        // When/Then
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.authenticate(request);
        });

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void testAuthenticate_WrongPassword() {
        // Given - create user first
        CreateUserRequest createRequest = new CreateUserRequest();
        createRequest.setEmail("service-wrongpw@example.com");
        createRequest.setPassword("correctpassword");
        createRequest.setFirstName("John");
        createRequest.setLastName("Doe");
        userService.createUser(createRequest);

        // When - try with wrong password
        LoginRequest request = new LoginRequest();
        request.setEmail("service-wrongpw@example.com");
        request.setPassword("wrongpassword");

        // Then
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            userService.authenticate(request);
        });

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void testCreateUser_SetsUserContext() {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("service-context@example.com");
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");

        // When
        userService.createUser(request);

        // Then - UserContext should be set
        assertNotNull(UserContext.getCurrentUser());
    }
}
