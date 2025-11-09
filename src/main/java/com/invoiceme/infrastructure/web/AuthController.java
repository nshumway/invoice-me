package com.invoiceme.infrastructure.web;

import com.invoiceme.application.common.ApiResponse;
import com.invoiceme.application.user.UserService;
import com.invoiceme.application.user.dto.AuthResponse;
import com.invoiceme.application.user.dto.CreateUserRequest;
import com.invoiceme.application.user.dto.LoginRequest;
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

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        UserResponse user = userService.authenticate(request);
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        AuthResponse authResponse = new AuthResponse(token, user);

        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }
}
