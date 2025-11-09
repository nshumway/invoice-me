package com.invoiceme.infrastructure.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private static final String TEST_SECRET = "test-secret-key-minimum-256-bits-required-for-hmac-sha-algorithm";
    private static final long TEST_EXPIRATION = 86400000L; // 24 hours

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(TEST_SECRET, TEST_EXPIRATION);
    }

    @Test
    void testGenerateToken() {
        // Given
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";

        // When
        String token = tokenProvider.generateToken(userId, email);

        // Then
        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts
    }

    @Test
    void testValidateToken() {
        // Given
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String token = tokenProvider.generateToken(userId, email);

        // When/Then
        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    void testValidateInvalidToken() {
        // When/Then
        assertFalse(tokenProvider.validateToken("invalid-token"));
        assertFalse(tokenProvider.validateToken(""));
        assertFalse(tokenProvider.validateToken(null));
    }

    @Test
    void testValidateMalformedToken() {
        // Given - token with wrong structure
        String malformedToken = "header.payload"; // Missing signature

        // When/Then
        assertFalse(tokenProvider.validateToken(malformedToken));
    }

    @Test
    void testExtractUserIdFromToken() {
        // Given
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String token = tokenProvider.generateToken(userId, email);

        // When
        UUID extractedId = tokenProvider.getUserIdFromToken(token);

        // Then
        assertEquals(userId, extractedId);
    }

    @Test
    void testExtractUserIdFromInvalidToken() {
        // When/Then
        assertThrows(JwtException.class, () -> {
            tokenProvider.getUserIdFromToken("invalid-token");
        });
    }

    @Test
    void testTokensAreUnique() throws InterruptedException {
        // Given
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";

        // When - generate two tokens for same user
        String token1 = tokenProvider.generateToken(userId, email);
        Thread.sleep(1000); // Ensure different timestamp (1 second)
        String token2 = tokenProvider.generateToken(userId, email);

        // Then - tokens should be different (due to different issued-at times)
        assertNotEquals(token1, token2);
    }

    @Test
    void testDifferentUsersGetDifferentTokens() {
        // Given
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        String email = "test@example.com";

        // When
        String token1 = tokenProvider.generateToken(userId1, email);
        String token2 = tokenProvider.generateToken(userId2, email);

        // Then
        assertNotEquals(token1, token2);
        assertEquals(userId1, tokenProvider.getUserIdFromToken(token1));
        assertEquals(userId2, tokenProvider.getUserIdFromToken(token2));
    }

    @Test
    void testTokenExpirationConfiguration() {
        // Given - provider with short expiration
        JwtTokenProvider shortExpirationProvider = new JwtTokenProvider(TEST_SECRET, 1000L); // 1 second
        UUID userId = UUID.randomUUID();
        String token = shortExpirationProvider.generateToken(userId, "test@example.com");

        // When - immediately validate
        boolean validNow = shortExpirationProvider.validateToken(token);

        // Then
        assertTrue(validNow);

        // Note: We don't test actual expiration in unit tests as it requires waiting
        // This should be covered in integration tests if needed
    }
}
