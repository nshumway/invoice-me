package com.invoiceme.infrastructure.persistence;

import com.invoiceme.application.common.UserContext;
import com.invoiceme.domain.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        UserContext.setCurrentUser(testUserId);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void testCreateUser() {
        // Given
        User user = new User();
        user.create("create@example.com", "hashedPassword", "John", "Doe");

        // When
        User saved = userRepository.save(user);

        // Then
        assertNotNull(saved.getId());
        assertEquals("create@example.com", saved.getEmail());
        assertEquals("hashedPassword", saved.getPasswordHash());
        assertEquals("John", saved.getFirstName());
        assertEquals("Doe", saved.getLastName());
        assertEquals("John Doe", saved.getFullName());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getCreatedBy());
        assertEquals(testUserId, saved.getCreatedBy());
        assertFalse(saved.getIsDeleted());
    }

    @Test
    void testFindByEmail() {
        // Given
        User user = new User();
        user.create("find@example.com", "hash", "Jane", "Doe");
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByEmailAndIsDeletedFalse("find@example.com");

        // Then
        assertTrue(found.isPresent());
        assertEquals("Jane", found.get().getFirstName());
        assertEquals("Doe", found.get().getLastName());
    }

    @Test
    void testFindByEmailNotFound() {
        // When
        Optional<User> found = userRepository.findByEmailAndIsDeletedFalse("nonexistent@example.com");

        // Then
        assertFalse(found.isPresent());
    }

    @Test
    void testExistsByEmail() {
        // Given
        User user = new User();
        user.create("exists@example.com", "hash", "Test", "User");
        userRepository.save(user);

        // When/Then
        assertTrue(userRepository.existsByEmailAndIsDeletedFalse("exists@example.com"));
        assertFalse(userRepository.existsByEmailAndIsDeletedFalse("notexists@example.com"));
    }

    @Test
    void testSoftDelete() {
        // Given
        User user = new User();
        user.create("delete@example.com", "hash", "Delete", "User");
        User saved = userRepository.save(user);

        // When - soft delete
        saved.markAsDeleted();
        userRepository.save(saved);

        // Then
        Optional<User> found = userRepository.findByEmailAndIsDeletedFalse("delete@example.com");
        assertFalse(found.isPresent());

        // But user still exists in database
        Optional<User> foundById = userRepository.findById(saved.getId());
        assertTrue(foundById.isPresent());
        assertTrue(foundById.get().getIsDeleted());
    }

    @Test
    void testEmailUniquenessForActiveUsers() {
        // Given
        User user1 = new User();
        user1.create("unique-constraint-test@example.com", "hash1", "User", "One");
        userRepository.save(user1);
        entityManager.flush();

        // When/Then - should throw exception due to unique constraint
        User user2 = new User();
        user2.create("unique-constraint-test@example.com", "hash2", "User", "Two");

        assertThrows(Exception.class, () -> {
            userRepository.save(user2);
            entityManager.flush(); // Force constraint check
        });
    }

    @Test
    void testEmailCanBeReusedAfterSoftDelete() {
        // Given
        User user1 = new User();
        user1.create("reuse@example.com", "hash1", "User", "One");
        User saved1 = userRepository.save(user1);

        // Soft delete first user
        saved1.markAsDeleted();
        userRepository.save(saved1);
        userRepository.flush();

        // When - create new user with same email
        User user2 = new User();
        user2.create("reuse@example.com", "hash2", "User", "Two");
        User saved2 = userRepository.save(user2);

        // Then - should succeed
        assertNotNull(saved2.getId());
        assertNotEquals(saved1.getId(), saved2.getId());
        assertEquals("reuse@example.com", saved2.getEmail());
    }
}
