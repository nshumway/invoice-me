package com.invoiceme.application.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 255, message = "First name must not exceed 255 characters")
    @Pattern(regexp = "^(?!.*(<|>|javascript:|data:|vbscript:|on\\w+\\s*=)).*$",
             message = "First name contains invalid characters or script content")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 255, message = "Last name must not exceed 255 characters")
    @Pattern(regexp = "^(?!.*(<|>|javascript:|data:|vbscript:|on\\w+\\s*=)).*$",
             message = "Last name contains invalid characters or script content")
    private String lastName;

    // Getters and setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
}
