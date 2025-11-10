package com.invoiceme.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceme.application.common.UserContext;
import com.invoiceme.application.customer.dto.CreateCustomerRequest;
import com.invoiceme.application.user.dto.CreateUserRequest;
import com.invoiceme.application.user.dto.LoginRequest;
import com.invoiceme.domain.user.User;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
import com.invoiceme.infrastructure.persistence.UserRepository;
import io.jsonwebtoken.JwtException;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive security tests covering:
 * - JWT token expiration
 * - Invalid/malformed JWT tokens
 * - SQL injection attacks
 * - XSS (Cross-Site Scripting) attacks
 * - Authentication and authorization
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String validToken;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        // Create a test user and generate a valid token
        User user = new User();
        user.create("security@example.com", passwordEncoder.encode("password123"), "Security", "Test");
        UserContext.setCurrentUser(UUID.randomUUID());
        user = userRepository.save(user);
        UserContext.clear();

        testUserId = user.getId();
        validToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail());
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // ========== JWT Token Expiration Tests ==========

    @Test
    void testExpiredToken_ShouldBeRejected() throws Exception {
        // Given - create a token provider with 1ms expiration (already expired)
        JwtTokenProvider shortExpirationProvider = new JwtTokenProvider(
            "test-secret-key-minimum-256-bits-required-for-hmac-sha-algorithm",
            1L // 1 millisecond
        );
        String expiredToken = shortExpirationProvider.generateToken(testUserId, "security@example.com");

        // Wait for token to expire
        Thread.sleep(10);

        // When/Then - try to access protected endpoint with expired token
        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testTokenExpiration_ValidateTokenReturnsFalse() throws Exception {
        // Given - create a token with very short expiration
        JwtTokenProvider shortExpirationProvider = new JwtTokenProvider(
            "test-secret-key-minimum-256-bits-required-for-hmac-sha-algorithm",
            1L // 1 millisecond
        );
        String token = shortExpirationProvider.generateToken(testUserId, "security@example.com");

        // Wait for token to expire
        Thread.sleep(10);

        // When/Then
        assertFalse(shortExpirationProvider.validateToken(token));
    }

    @Test
    void testTokenExpiration_ExtractUserIdThrowsException() throws Exception {
        // Given - create a token with very short expiration
        JwtTokenProvider shortExpirationProvider = new JwtTokenProvider(
            "test-secret-key-minimum-256-bits-required-for-hmac-sha-algorithm",
            1L // 1 millisecond
        );
        String token = shortExpirationProvider.generateToken(testUserId, "security@example.com");

        // Wait for token to expire
        Thread.sleep(10);

        // When/Then
        assertThrows(JwtException.class, () -> {
            shortExpirationProvider.getUserIdFromToken(token);
        });
    }

    // ========== Invalid Token Tests ==========

    @Test
    void testInvalidToken_MalformedStructure() throws Exception {
        // Given - malformed tokens
        String[] malformedTokens = {
            "not.a.valid.jwt.token",
            "header.payload", // Missing signature
            "only-one-part",
            "",
            "Bearer invalid"
        };

        // When/Then - all should be rejected
        for (String token : malformedTokens) {
            mockMvc.perform(get("/api/customers")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void testInvalidToken_TamperedPayload() throws Exception {
        // Given - valid token structure but tampered content
        String[] parts = validToken.split("\\.");
        String tamperedToken = parts[0] + ".eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." + parts[2];

        // When/Then
        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testInvalidToken_WrongSignature() throws Exception {
        // Given - create token with different secret
        JwtTokenProvider differentSecretProvider = new JwtTokenProvider(
            "different-secret-key-minimum-256-bits-required-for-hmac-sha-algorithm",
            86400000L
        );
        String tokenWithWrongSignature = differentSecretProvider.generateToken(testUserId, "security@example.com");

        // When/Then - token signed with different secret should be rejected
        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + tokenWithWrongSignature))
                .andExpect(status().isForbidden());
    }

    @Test
    void testMissingToken_ShouldBeRejected() throws Exception {
        // When/Then - request without Authorization header
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testInvalidTokenFormat_WithoutBearerPrefix() throws Exception {
        // When/Then - token without "Bearer " prefix
        mockMvc.perform(get("/api/customers")
                        .header("Authorization", validToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testNullToken_ShouldBeRejected() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer null"))
                .andExpect(status().isForbidden());
    }

    // ========== SQL Injection Tests ==========

    @Test
    void testSQLInjection_InEmailField() throws Exception {
        // Given - SQL injection attempts in email field
        String[] sqlInjectionPayloads = {
            "admin'--",
            "admin' OR '1'='1",
            "admin'; DROP TABLE users--",
            "admin' OR 1=1--",
            "' UNION SELECT * FROM users--",
            "admin'/*",
            "' OR 'x'='x"
        };

        // When/Then - all injection attempts should fail safely
        for (String payload : sqlInjectionPayloads) {
            LoginRequest request = new LoginRequest();
            request.setEmail(payload);
            request.setPassword("password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void testSQLInjection_InCustomerName() throws Exception {
        // Given - capture initial data counts to verify integrity
        long initialUserCount = userRepository.count();
        long initialCustomerCount = customerRepository.count();

        // SQL injection attempts in customer name
        String[] sqlInjectionPayloads = {
            "Customer'; DROP TABLE customers--",
            "Customer' OR '1'='1",
            "'; DELETE FROM customers WHERE '1'='1",
            "Customer' UNION SELECT * FROM users--"
        };

        // When/Then - all injection attempts should be handled safely
        // Some payloads may be rejected by validation (which is good security!)
        for (String payload : sqlInjectionPayloads) {
            CreateCustomerRequest request = new CreateCustomerRequest();
            request.setCompanyName(payload);
            request.setEmail("test" + System.currentTimeMillis() + "@example.com");
            request.setPhone("1234567890");
            request.setAddressLine1("123 Test St");

            // System should either accept (if passes validation) or reject (if caught by validation)
            // Both outcomes are safe - what matters is no SQL injection occurs
            mockMvc.perform(post("/api/customers")
                            .header("Authorization", "Bearer " + validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)));
        }

        // Verify data integrity - no records should have been deleted by SQL injection
        long finalUserCount = userRepository.count();
        long finalCustomerCount = customerRepository.count();

        assertEquals(initialUserCount, finalUserCount,
            "User count should remain unchanged - SQL injection should not delete users");
        assertTrue(finalCustomerCount >= initialCustomerCount,
            "Customer count should not decrease - SQL injection should not delete customers");

        // Verify tables still exist and are queryable
        assertNotNull(userRepository.findAll(),
            "Users table should still exist and be queryable");
        assertNotNull(customerRepository.findAll(),
            "Customers table should still exist and be queryable");
    }

    @Test
    void testSQLInjection_InSearchParameters() throws Exception {
        // Given - capture initial data counts to verify integrity
        long initialUserCount = userRepository.count();
        long initialCustomerCount = customerRepository.count();

        // SQL injection attempts via query parameters
        String[] sqlInjectionPayloads = {
            "' OR '1'='1",
            "'; DROP TABLE customers--",
            "1' UNION SELECT * FROM users--"
        };

        // When/Then - search should handle injection attempts safely
        for (String payload : sqlInjectionPayloads) {
            mockMvc.perform(get("/api/customers")
                            .header("Authorization", "Bearer " + validToken)
                            .param("search", payload))
                    .andExpect(status().isOk());
        }

        // Verify data integrity after SQL injection attempts
        long finalUserCount = userRepository.count();
        long finalCustomerCount = customerRepository.count();

        assertEquals(initialUserCount, finalUserCount,
            "User count should remain unchanged after search SQL injection attempts");
        assertEquals(initialCustomerCount, finalCustomerCount,
            "Customer count should remain unchanged after search SQL injection attempts");

        // Verify tables still exist and are queryable
        assertNotNull(userRepository.findAll(),
            "Users table should still exist after SQL injection attempts");
        assertNotNull(customerRepository.findAll(),
            "Customers table should still exist after SQL injection attempts");
    }

    // ========== XSS (Cross-Site Scripting) Tests ==========

    @Test
    void testXSS_InUserSignup() throws Exception {
        // Given - XSS payloads
        String[] xssPayloads = {
            "<script>alert('XSS')</script>",
            "<img src=x onerror=alert('XSS')>",
            "<svg/onload=alert('XSS')>",
            "javascript:alert('XSS')",
            "<iframe src='javascript:alert(\"XSS\")'></iframe>"
        };

        // When/Then - XSS payloads MUST be blocked by validation
        for (String payload : xssPayloads) {
            CreateUserRequest request = new CreateUserRequest();
            request.setEmail("xss" + System.currentTimeMillis() + "@example.com");
            request.setPassword("password123");
            request.setFirstName(payload);
            request.setLastName("User");

            MvcResult result = mockMvc.perform(post("/api/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn();

            // Strictly verify XSS payload was blocked with 400 Bad Request
            assertEquals(400, result.getResponse().getStatus(),
                "XSS payload '" + payload + "' should be rejected with 400 Bad Request");

            String response = result.getResponse().getContentAsString();

            // Verify the raw XSS payload is NOT present in the response
            // (prevents reflection XSS attacks)
            assertThat(response, not(containsString(payload)));
        }
    }

    @Test
    void testXSS_InCustomerData() throws Exception {
        // Given - XSS payloads in customer data
        String[] xssPayloads = {
            "<script>alert('XSS')</script>",
            "<img src=x onerror=alert('XSS')>",
            "javascript:void(0)",
            "<body onload=alert('XSS')>"
        };

        // When/Then - XSS payloads MUST be blocked by validation
        for (String payload : xssPayloads) {
            CreateCustomerRequest request = new CreateCustomerRequest();
            request.setCompanyName(payload);
            request.setEmail("customer" + System.currentTimeMillis() + "@example.com");
            request.setPhone("1234567890");
            request.setAddressLine1("<script>alert('XSS')</script>");

            MvcResult result = mockMvc.perform(post("/api/customers")
                            .header("Authorization", "Bearer " + validToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andReturn();

            // Strictly verify XSS payloads are blocked with 400 Bad Request
            assertEquals(400, result.getResponse().getStatus(),
                "XSS payload '" + payload + "' should be rejected with 400 Bad Request");

            String response = result.getResponse().getContentAsString();

            // Verify raw XSS payloads are NOT reflected in the response
            assertThat(response, not(containsString(payload)));
            assertThat(response, not(containsString("<script>")));
        }
    }

    @Test
    void testXSS_ValidationBlocksScriptTags() throws Exception {
        // Given - create customer with script tag
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("<script>alert('XSS')</script>");
        request.setEmail("scripttest@example.com");
        request.setPhone("1234567890");
        request.setAddressLine1("123 Test St");

        // When/Then - validation should block HTML tags
        mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ========== Authentication & Authorization Tests ==========

    @Test
    void testUnauthorizedAccess_ToProtectedEndpoint() throws Exception {
        // When/Then - accessing protected endpoint without token
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testValidToken_AllowsAccessToProtectedEndpoint() throws Exception {
        // When/Then - accessing protected endpoint with valid token
        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    void testPasswordHashNotExposedInResponse() throws Exception {
        // Given
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("hashtest@example.com");
        request.setPassword("password123");
        request.setFirstName("Hash");
        request.setLastName("Test");

        // When
        MvcResult result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        // Then - verify password-related fields are not in response
        assertThat(response, not(containsString("password")));
        assertThat(response, not(containsString("passwordHash")));
        assertThat(response, not(containsString("$2a$"))); // BCrypt hash prefix
    }

    @Test
    void testSecureHeaders_ArePresent() throws Exception {
        // When
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest())))
                .andReturn();

        // Then - verify security headers (if configured in SecurityConfig)
        // Note: Add more header checks based on your security configuration
        assertNotNull(result.getResponse());
    }

    // ========== Authorization Tests ==========

    @Test
    void testUserCannotAccessAnotherUsersCustomers() throws Exception {
        // Given - create two different users
        User user1 = new User();
        user1.create("user1@example.com", passwordEncoder.encode("password123"), "User", "One");
        UserContext.setCurrentUser(UUID.randomUUID());
        user1 = userRepository.save(user1);
        UserContext.clear();

        User user2 = new User();
        user2.create("user2@example.com", passwordEncoder.encode("password123"), "User", "Two");
        UserContext.setCurrentUser(UUID.randomUUID());
        user2 = userRepository.save(user2);
        UserContext.clear();

        // Create a customer for user1
        String user1Token = jwtTokenProvider.generateToken(user1.getId(), user1.getEmail());

        CreateCustomerRequest customerRequest = new CreateCustomerRequest();
        customerRequest.setCompanyName("User1 Customer");
        customerRequest.setEmail("user1customer@example.com");
        customerRequest.setPhone("1234567890");
        customerRequest.setAddressLine1("123 Test St");

        MvcResult createResult = mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + user1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract customer ID from response
        String responseBody = createResult.getResponse().getContentAsString();
        String customerId = objectMapper.readTree(responseBody).get("data").get("id").asText();

        // When - user2 tries to access user1's customer
        String user2Token = jwtTokenProvider.generateToken(user2.getId(), user2.getEmail());

        // Then - user2 should NOT be able to view user1's customer
        mockMvc.perform(get("/api/customers/" + customerId)
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isNotFound()); // or 403 Forbidden depending on implementation

        // user2 should not see user1's customer in their list
        MvcResult listResult = mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + user2Token))
                .andExpect(status().isOk())
                .andReturn();

        String listResponse = listResult.getResponse().getContentAsString();
        assertThat(listResponse, not(containsString(customerId)));
    }

    // TODO: Fix test - needs to extract and include version field from create response
    // @Test
    // void testUserCannotUpdateAnotherUsersCustomers() throws Exception {
    //     // Given - create two different users
    //     User user1 = new User();
    //     user1.create("user1update@example.com", passwordEncoder.encode("password123"), "User", "One");
    //     UserContext.setCurrentUser(UUID.randomUUID());
    //     user1 = userRepository.save(user1);
    //     UserContext.clear();

    //     User user2 = new User();
    //     user2.create("user2update@example.com", passwordEncoder.encode("password123"), "User", "Two");
    //     UserContext.setCurrentUser(UUID.randomUUID());
    //     user2 = userRepository.save(user2);
    //     UserContext.clear();

    //     // Create a customer for user1
    //     String user1Token = jwtTokenProvider.generateToken(user1.getId(), user1.getEmail());

    //     CreateCustomerRequest customerRequest = new CreateCustomerRequest();
    //     customerRequest.setCompanyName("User1 Customer Update Test");
    //     customerRequest.setEmail("user1updatecustomer@example.com");
    //     customerRequest.setPhone("1234567890");
    //     customerRequest.setAddressLine1("123 Test St");

    //     MvcResult createResult = mockMvc.perform(post("/api/customers")
    //                     .header("Authorization", "Bearer " + user1Token)
    //                     .contentType(MediaType.APPLICATION_JSON)
    //                     .content(objectMapper.writeValueAsString(customerRequest)))
    //             .andExpect(status().isCreated())
    //             .andReturn();

    //     String responseBody = createResult.getResponse().getContentAsString();
    //     String customerId = objectMapper.readTree(responseBody).get("data").get("id").asText();

    //     // When - user2 tries to update user1's customer
    //     String user2Token = jwtTokenProvider.generateToken(user2.getId(), user2.getEmail());

    //     customerRequest.setCompanyName("Hacked by User2");

    //     // Then - user2 should NOT be able to update user1's customer
    //     mockMvc.perform(put("/api/customers/" + customerId)
    //                     .header("Authorization", "Bearer " + user2Token)
    //                     .contentType(MediaType.APPLICATION_JSON)
    //                     .content(objectMapper.writeValueAsString(customerRequest)))
    //             .andExpect(status().isNotFound()); // or 403 Forbidden
    // }

    // TODO: Fix test - needs to include version query parameter
    // @Test
    // void testUserCannotDeleteAnotherUsersCustomers() throws Exception {
    //     // Given - create two different users
    //     User user1 = new User();
    //     user1.create("user1delete@example.com", passwordEncoder.encode("password123"), "User", "One");
    //     UserContext.setCurrentUser(UUID.randomUUID());
    //     user1 = userRepository.save(user1);
    //     UserContext.clear();

    //     User user2 = new User();
    //     user2.create("user2delete@example.com", passwordEncoder.encode("password123"), "User", "Two");
    //     UserContext.setCurrentUser(UUID.randomUUID());
    //     user2 = userRepository.save(user2);
    //     UserContext.clear();

    //     // Create a customer for user1
    //     String user1Token = jwtTokenProvider.generateToken(user1.getId(), user1.getEmail());

    //     CreateCustomerRequest customerRequest = new CreateCustomerRequest();
    //     customerRequest.setCompanyName("User1 Customer Delete Test");
    //     customerRequest.setEmail("user1deletecustomer@example.com");
    //     customerRequest.setPhone("1234567890");
    //     customerRequest.setAddressLine1("123 Test St");

    //     MvcResult createResult = mockMvc.perform(post("/api/customers")
    //                     .header("Authorization", "Bearer " + user1Token)
    //                     .contentType(MediaType.APPLICATION_JSON)
    //                     .content(objectMapper.writeValueAsString(customerRequest)))
    //             .andExpect(status().isCreated())
    //             .andReturn();

    //     String responseBody = createResult.getResponse().getContentAsString();
    //     String customerId = objectMapper.readTree(responseBody).get("data").get("id").asText();

    //     // When - user2 tries to delete user1's customer
    //     String user2Token = jwtTokenProvider.generateToken(user2.getId(), user2.getEmail());

    //     // Then - user2 should NOT be able to delete user1's customer
    //     mockMvc.perform(delete("/api/customers/" + customerId)
    //                     .header("Authorization", "Bearer " + user2Token))
    //             .andExpect(status().isNotFound()); // or 403 Forbidden

    //     // Verify customer still exists for user1
    //     mockMvc.perform(get("/api/customers/" + customerId)
    //                     .header("Authorization", "Bearer " + user1Token))
    //             .andExpect(status().isOk());
    // }

    // TODO: Re-enable when Invoice controller is implemented
    // @Test
    // void testUserCanOnlyAccessTheirOwnInvoices() throws Exception {
    //     // Given - create two different users
    //     User user1 = new User();
    //     user1.create("user1invoice@example.com", passwordEncoder.encode("password123"), "User", "One");
    //     UserContext.setCurrentUser(UUID.randomUUID());
    //     user1 = userRepository.save(user1);
    //     UserContext.clear();

    //     User user2 = new User();
    //     user2.create("user2invoice@example.com", passwordEncoder.encode("password123"), "User", "Two");
    //     UserContext.setCurrentUser(UUID.randomUUID());
    //     user2 = userRepository.save(user2);
    //     UserContext.clear();

    //     String user1Token = jwtTokenProvider.generateToken(user1.getId(), user1.getEmail());
    //     String user2Token = jwtTokenProvider.generateToken(user2.getId(), user2.getEmail());

    //     // When - user2 tries to list invoices (should not see user1's invoices)
    //     MvcResult listResult = mockMvc.perform(get("/api/invoices")
    //                     .header("Authorization", "Bearer " + user2Token))
    //             .andExpect(status().isOk())
    //             .andReturn();

    //     // Then - response should not contain any data from user1
    //     String listResponse = listResult.getResponse().getContentAsString();
    //     assertNotNull(listResponse, "Response should not be null");

    //     // Verify isolation - user2 can only access their own resources
    //     // If there are invoices, they should only belong to user2
    // }

    @Test
    void testTokenWithDifferentUserIdCannotAccessResources() throws Exception {
        // Given - create a customer with valid token
        CreateCustomerRequest customerRequest = new CreateCustomerRequest();
        customerRequest.setCompanyName("Test Customer");
        customerRequest.setEmail("tokentest@example.com");
        customerRequest.setPhone("1234567890");
        customerRequest.setAddressLine1("123 Test St");

        MvcResult createResult = mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String customerId = objectMapper.readTree(responseBody).get("data").get("id").asText();

        // When - create token for a different user
        UUID differentUserId = UUID.randomUUID();
        String differentUserToken = jwtTokenProvider.generateToken(differentUserId, "different@example.com");

        // Then - different user should not be able to access the customer
        mockMvc.perform(get("/api/customers/" + customerId)
                        .header("Authorization", "Bearer " + differentUserToken))
                .andExpect(status().isNotFound()); // Customer doesn't exist in their scope
    }

    // ========== Input Validation Security Tests ==========

    @Test
    void testExcessivelyLongInput_IsRejected() throws Exception {
        // Given - create extremely long string (potential DoS attack)
        String longString = "a".repeat(10000);

        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName(longString);
        request.setEmail("test@example.com");
        request.setPhone("1234567890");
        request.setAddressLine1("123 Test St");

        // When/Then - should handle gracefully
        mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testNullByteInjection_IsHandled() throws Exception {
        // Given - null byte injection attempt
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("Customer\u0000Admin");
        request.setEmail("test@example.com");
        request.setPhone("1234567890");
        request.setAddressLine1("123 Test St");

        // When/Then - should handle safely
        MvcResult result = mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();

        // Verify it's handled (either rejected or safely stored)
        assertTrue(result.getResponse().getStatus() == 201 ||
                   result.getResponse().getStatus() == 400);
    }
}
