package com.invoiceme.domain.customer;

import com.invoiceme.application.common.UserContext;
import com.invoiceme.domain.common.exceptions.ValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Customer entity domain logic.
 * These tests focus on pure domain behavior without infrastructure dependencies.
 */
class CustomerTest {

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

    // === CREATE TESTS ===

    @Test
    void testCreateCustomerSuccess() {
        Customer customer = new Customer();

        // Validate
        assertDoesNotThrow(() ->
            customer.validateForCreate("Acme Corp", "contact@acme.com"));

        // Create
        customer.create(
            "Acme Corp",
            "John",
            "Doe",
            "contact@acme.com",
            "555-1234",
            "123 Main St",
            "Suite 100",
            "Boston",
            "MA",
            "02101",
            "USA"
        );

        // Assert all fields set correctly
        assertEquals("Acme Corp", customer.getCompanyName());
        assertEquals("contact@acme.com", customer.getEmail());
        assertEquals("John", customer.getContactFirstName());
        assertEquals("Doe", customer.getContactLastName());
        assertEquals("555-1234", customer.getPhone());
        assertEquals("123 Main St", customer.getAddressLine1());
        assertEquals("Suite 100", customer.getAddressLine2());
        assertEquals("Boston", customer.getCity());
        assertEquals("MA", customer.getState());
        assertEquals("02101", customer.getZipCode());
        assertEquals("USA", customer.getCountry());

        // Assert statistics initialized
        assertEquals(0, customer.getDraftInvoiceCount());
        assertEquals(0, customer.getSentInvoiceCount());
        assertEquals(0, customer.getPaidInvoiceCount());
        assertEquals(BigDecimal.ZERO, customer.getTotalOutstanding());
    }

    @Test
    void testValidateForCreateThrowsExceptionWhenCompanyNameBlank() {
        Customer customer = new Customer();

        assertThrows(ValidationException.class, () ->
            customer.validateForCreate("", "test@example.com"));
    }

    @Test
    void testValidateForCreateThrowsExceptionWhenCompanyNameNull() {
        Customer customer = new Customer();

        assertThrows(ValidationException.class, () ->
            customer.validateForCreate(null, "test@example.com"));
    }

    @Test
    void testValidateForCreateThrowsExceptionWhenEmailBlank() {
        Customer customer = new Customer();

        assertThrows(ValidationException.class, () ->
            customer.validateForCreate("Test Corp", ""));
    }

    @Test
    void testValidateForCreateThrowsExceptionWhenEmailNull() {
        Customer customer = new Customer();

        assertThrows(ValidationException.class, () ->
            customer.validateForCreate("Test Corp", null));
    }

    @Test
    void testCreateWithMinimalData() {
        Customer customer = new Customer();

        customer.validateForCreate("Minimal Corp", "minimal@test.com");
        customer.create(
            "Minimal Corp",
            null,  // No first name
            null,  // No last name
            "minimal@test.com",
            null,  // No phone
            null,  // No address
            null,
            null,
            null,
            null,
            null
        );

        assertEquals("Minimal Corp", customer.getCompanyName());
        assertEquals("minimal@test.com", customer.getEmail());
        assertNull(customer.getContactFirstName());
        assertNull(customer.getPhone());
        assertNull(customer.getAddressLine1());
    }

    // === UPDATE TESTS ===

    @Test
    void testUpdateCustomerSuccess() {
        // Create initial customer
        Customer customer = new Customer();
        customer.create(
            "Original Corp",
            "John",
            "Doe",
            "original@test.com",
            "555-1234",
            null, null, null, null, null, null
        );

        // Validate update
        assertDoesNotThrow(() ->
            customer.validateForUpdate("Updated Corp", "updated@test.com", false));

        // Update
        customer.update(
            "Updated Corp",
            "Jane",
            "Smith",
            "updated@test.com",
            "555-5678",
            "456 New St",
            null,
            "Cambridge",
            "MA",
            "02139",
            "USA"
        );

        // Assert updates applied
        assertEquals("Updated Corp", customer.getCompanyName());
        assertEquals("updated@test.com", customer.getEmail());
        assertEquals("Jane", customer.getContactFirstName());
        assertEquals("Smith", customer.getContactLastName());
        assertEquals("555-5678", customer.getPhone());
        assertEquals("456 New St", customer.getAddressLine1());
        assertEquals("Cambridge", customer.getCity());
    }

    @Test
    void testValidateForUpdateThrowsExceptionWhenCompanyNameBlank() {
        Customer customer = new Customer();

        assertThrows(ValidationException.class, () ->
            customer.validateForUpdate("", "test@example.com", false));
    }

    @Test
    void testValidateForUpdateThrowsExceptionWhenCompanyNameNull() {
        Customer customer = new Customer();

        assertThrows(ValidationException.class, () ->
            customer.validateForUpdate(null, "test@example.com", false));
    }

    @Test
    void testValidateForUpdateThrowsExceptionWhenEmailBlank() {
        Customer customer = new Customer();

        assertThrows(ValidationException.class, () ->
            customer.validateForUpdate("Test Corp", "", false));
    }

    @Test
    void testValidateForUpdateThrowsExceptionWhenEmailNull() {
        Customer customer = new Customer();

        assertThrows(ValidationException.class, () ->
            customer.validateForUpdate("Test Corp", null, false));
    }

    // === DELETE TESTS ===

    @Test
    void testDeleteCustomer() {
        Customer customer = new Customer();
        customer.create(
            "To Delete Corp",
            null, null,
            "delete@test.com",
            null, null, null, null, null, null, null
        );

        // Initially not deleted
        assertFalse(customer.getIsDeleted());

        // Perform delete
        customer.delete();

        // Now should be soft deleted
        assertTrue(customer.getIsDeleted());
        assertNotNull(customer.getDeletedAt());
    }

    // === STATISTICS HELPER METHODS (Deprecated) ===

    @Test
    @SuppressWarnings("deprecation")
    void testIncrementDecrementDraftInvoiceCount() {
        Customer customer = new Customer();
        customer.create(
            "Test Corp",
            null, null,
            "test@test.com",
            null, null, null, null, null, null, null
        );

        assertEquals(0, customer.getDraftInvoiceCount());

        customer.incrementDraftInvoiceCount();
        assertEquals(1, customer.getDraftInvoiceCount());

        customer.incrementDraftInvoiceCount();
        assertEquals(2, customer.getDraftInvoiceCount());

        customer.decrementDraftInvoiceCount();
        assertEquals(1, customer.getDraftInvoiceCount());
    }

    @Test
    @SuppressWarnings("deprecation")
    void testIncrementDecrementSentInvoiceCount() {
        Customer customer = new Customer();
        customer.create(
            "Test Corp",
            null, null,
            "test@test.com",
            null, null, null, null, null, null, null
        );

        assertEquals(0, customer.getSentInvoiceCount());

        customer.incrementSentInvoiceCount();
        assertEquals(1, customer.getSentInvoiceCount());

        customer.decrementSentInvoiceCount();
        assertEquals(0, customer.getSentInvoiceCount());
    }

    @Test
    @SuppressWarnings("deprecation")
    void testIncrementDecrementPaidInvoiceCount() {
        Customer customer = new Customer();
        customer.create(
            "Test Corp",
            null, null,
            "test@test.com",
            null, null, null, null, null, null, null
        );

        assertEquals(0, customer.getPaidInvoiceCount());

        customer.incrementPaidInvoiceCount();
        assertEquals(1, customer.getPaidInvoiceCount());

        customer.decrementPaidInvoiceCount();
        assertEquals(0, customer.getPaidInvoiceCount());
    }

    @Test
    @SuppressWarnings("deprecation")
    void testAddSubtractTotalOutstanding() {
        Customer customer = new Customer();
        customer.create(
            "Test Corp",
            null, null,
            "test@test.com",
            null, null, null, null, null, null, null
        );

        assertEquals(BigDecimal.ZERO, customer.getTotalOutstanding());

        customer.addToTotalOutstanding(new BigDecimal("100.50"));
        assertEquals(new BigDecimal("100.50"), customer.getTotalOutstanding());

        customer.addToTotalOutstanding(new BigDecimal("50.25"));
        assertEquals(new BigDecimal("150.75"), customer.getTotalOutstanding());

        customer.subtractFromTotalOutstanding(new BigDecimal("30.00"));
        assertEquals(new BigDecimal("120.75"), customer.getTotalOutstanding());
    }
}
