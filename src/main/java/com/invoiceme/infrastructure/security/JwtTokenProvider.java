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
        // Validate JWT secret meets minimum security requirements
        validateSecretKey(secret);
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    /**
     * Validates that the JWT secret meets minimum security requirements.
     * The secret must be at least 256 bits (32 bytes) for HS256 algorithm.
     *
     * @param secret the JWT secret to validate
     * @throws IllegalArgumentException if secret doesn't meet requirements
     */
    private void validateSecretKey(String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "JWT secret is required. Please set JWT_SECRET environment variable.");
        }

        // HS256 requires at least 256 bits (32 bytes)
        final int MIN_SECRET_LENGTH_BYTES = 32;
        byte[] secretBytes = secret.getBytes();

        if (secretBytes.length < MIN_SECRET_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                String.format("JWT secret must be at least %d bytes (%d bits) for HS256 algorithm. " +
                             "Current length: %d bytes. " +
                             "Please use a stronger secret key.",
                             MIN_SECRET_LENGTH_BYTES, MIN_SECRET_LENGTH_BYTES * 8, secretBytes.length));
        }

        // Additional check: warn if secret appears to be weak (all same character, sequential, etc.)
        if (isWeakSecret(secret)) {
            // Log warning but don't fail - this is just a hint
            System.err.println("WARNING: JWT secret appears to use a simple pattern. " +
                             "For production, use a cryptographically random secret.");
        }
    }

    /**
     * Checks if secret uses obviously weak patterns.
     * This is a basic check - for production use proper key management.
     */
    private boolean isWeakSecret(String secret) {
        // Check if all characters are the same
        if (secret.chars().distinct().count() <= 3) {
            return true;
        }

        // Check for sequential patterns (e.g., "12345678...")
        boolean sequential = true;
        for (int i = 1; i < Math.min(secret.length(), 10); i++) {
            if (secret.charAt(i) != secret.charAt(i - 1) + 1) {
                sequential = false;
                break;
            }
        }

        return sequential;
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
