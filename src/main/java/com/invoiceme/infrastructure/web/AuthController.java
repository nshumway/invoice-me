package com.invoiceme.infrastructure.web;

import com.invoiceme.application.common.ApiResponse;
import com.invoiceme.application.user.UserService;
import com.invoiceme.application.user.dto.AuthResponse;
import com.invoiceme.application.user.dto.CreateUserRequest;
import com.invoiceme.application.user.dto.LoginRequest;
import com.invoiceme.application.user.dto.UserResponse;
import com.invoiceme.infrastructure.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account and returns a JWT token for authentication"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "User successfully created",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = AuthResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input or email already exists",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponse>> signup(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = userService.createUser(request);
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        AuthResponse authResponse = new AuthResponse(token, user);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", authResponse));
    }

    @Operation(
        summary = "Authenticate user",
        description = "Authenticates user credentials and returns a JWT token"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = AuthResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid credentials",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        UserResponse user = userService.authenticate(request);
        String token = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
        AuthResponse authResponse = new AuthResponse(token, user);

        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }
}
