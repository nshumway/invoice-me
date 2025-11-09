package com.invoiceme.application.user;

import com.invoiceme.application.common.UserContext;
import com.invoiceme.application.user.dto.CreateUserRequest;
import com.invoiceme.application.user.dto.LoginRequest;
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

        // Generate a temporary UUID for the user before saving
        // This is needed for the audit trail, as the user is creating themselves
        UUID tempUserId = UUID.randomUUID();
        user.setId(tempUserId);

        // Set UserContext to user's own ID for audit trail
        // (This is a special case - normally UserContext is set by JWT filter)
        UserContext.setCurrentUser(tempUserId);

        userRepository.save(user);

        return userMapper.toResponse(user);
    }

    public UserResponse authenticate(LoginRequest request) {
        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow(() -> new ValidationException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ValidationException("Invalid email or password");
        }

        return userMapper.toResponse(user);
    }
}
