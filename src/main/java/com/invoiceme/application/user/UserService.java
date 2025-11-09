package com.invoiceme.application.user;

import com.invoiceme.application.common.UserContext;
import com.invoiceme.application.user.dto.CreateUserRequest;
import com.invoiceme.application.user.dto.LoginRequest;
import com.invoiceme.application.user.dto.UserResponse;
import com.invoiceme.domain.common.exceptions.ValidationException;
import com.invoiceme.domain.user.User;
import com.invoiceme.infrastructure.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        logger.info("Creating new user with email: {}", request.getEmail());
        logger.debug("Create user request: email={}, firstName={}, lastName={}",
                    request.getEmail(), request.getFirstName(), request.getLastName());

        try {
            // Check email uniqueness
            if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
                logger.warn("Attempt to create user with duplicate email: {}", request.getEmail());
                throw new ValidationException("User with email " + request.getEmail() + " already exists");
            }

            // Create user
            User user = new User();
            String passwordHash = passwordEncoder.encode(request.getPassword());
            user.create(request.getEmail(), passwordHash, request.getFirstName(), request.getLastName());

            // Generate a temporary UUID for the user before saving
            // This is needed for the audit trail, as the user is creating themselves
            UUID tempUserId = UUID.randomUUID();
            user.setId(tempUserId);

            // Set UserContext to user's own ID for audit trail
            // (This is a special case - normally UserContext is set by JWT filter)
            UserContext.setCurrentUser(tempUserId);

            userRepository.save(user);

            logger.info("Successfully created user: id={}, email={}", user.getId(), user.getEmail());
            return userMapper.toResponse(user);
        } catch (ValidationException e) {
            logger.error("Validation failed while creating user: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error creating user with email: {}", request.getEmail(), e);
            throw e;
        }
    }

    public UserResponse authenticate(LoginRequest request) {
        logger.info("Authentication attempt for email: {}", request.getEmail());

        try {
            User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                    .orElseThrow(() -> {
                        logger.warn("Authentication failed: user not found for email: {}", request.getEmail());
                        return new ValidationException("Invalid email or password");
                    });

            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                logger.warn("Authentication failed: invalid password for email: {}", request.getEmail());
                throw new ValidationException("Invalid email or password");
            }

            logger.info("Successfully authenticated user: id={}, email={}", user.getId(), user.getEmail());
            return userMapper.toResponse(user);
        } catch (ValidationException e) {
            // Don't log full exception for authentication failures (security best practice)
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during authentication for email: {}", request.getEmail(), e);
            throw e;
        }
    }
}
