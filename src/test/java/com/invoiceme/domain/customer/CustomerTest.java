package com.invoiceme.domain.customer;

import com.invoiceme.application.customer.dto.CreateCustomerRequest;
import com.invoiceme.application.customer.dto.UpdateCustomerRequest;
import com.invoiceme.domain.common.exceptions.ValidationException;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerTest {

    @Mock
    private CustomerRepository customerRepository;

    // === CREATE TESTS ===

    @Test
    void testCreateCustomerSuccess() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("Acme Corp");
        request.setEmail("contact@acme.com");
        request.setContactFirstName("John");
        request.setContactLastName("Doe");
        request.setPhone("555-1234");
        request.setAddressLine1("123 Main St");
        request.setCity("Boston");
        request.setState("MA");
        request.setZipCode("02101");
        request.setCountry("USA");

        when(customerRepository.existsByEmailAndIsDeletedFalse("contact@acme.com"))
                .thenReturn(false);

        Customer customer = new Customer();
        customer.beforeCreate(request, customerRepository);
        customer.create(request);
        customer.afterCreate();

        assertEquals("Acme Corp", customer.getCompanyName());
        assertEquals("contact@acme.com", customer.getEmail());
        assertEquals("John", customer.getContactFirstName());
        assertEquals("Doe", customer.getContactLastName());
        assertEquals("555-1234", customer.getPhone());
        assertEquals("123 Main St", customer.getAddressLine1());
        assertEquals("Boston", customer.getCity());
        assertEquals("MA", customer.getState());
        assertEquals("02101", customer.getZipCode());
        assertEquals("USA", customer.getCountry());
        assertEquals(0, customer.getDraftInvoiceCount());
        assertEquals(0, customer.getSentInvoiceCount());
        assertEquals(0, customer.getPaidInvoiceCount());
        assertEquals(BigDecimal.ZERO, customer.getTotalOutstanding());
    }

    @Test
    void testBeforeCreateThrowsExceptionWhenCompanyNameBlank() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("");
        request.setEmail("test@example.com");

        Customer customer = new Customer();

        assertThrows(ValidationException.class, () ->
            customer.beforeCreate(request, customerRepository));
    }

    @Test
    void testBeforeCreateThrowsExceptionWhenCompanyNameNull() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName(null);
        request.setEmail("test@example.com");

        Customer customer = new Customer();

        assertThrows(ValidationException.class, () ->
            customer.beforeCreate(request, customerRepository));
    }

    @Test
    void testBeforeCreateThrowsExceptionWhenEmailBlank() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("Test Corp");
        request.setEmail("");

        Customer customer = new Customer();

        assertThrows(ValidationException.class, () ->
            customer.beforeCreate(request, customerRepository));
    }

    @Test
    void testBeforeCreateThrowsExceptionWhenEmailNull() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("Test Corp");
        request.setEmail(null);

        Customer customer = new Customer();

        assertThrows(ValidationException.class, () ->
            customer.beforeCreate(request, customerRepository));
    }

    @Test
    void testBeforeCreateThrowsExceptionWhenEmailAlreadyExists() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("Test Corp");
        request.setEmail("existing@example.com");

        when(customerRepository.existsByEmailAndIsDeletedFalse("existing@example.com"))
                .thenReturn(true);

        Customer customer = new Customer();

        ValidationException exception = assertThrows(ValidationException.class, () ->
            customer.beforeCreate(request, customerRepository));

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void testCreateWithNullOptionalFields() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("Minimal Corp");
        request.setEmail("minimal@example.com");
        // All other fields null

        when(customerRepository.existsByEmailAndIsDeletedFalse("minimal@example.com"))
                .thenReturn(false);

        Customer customer = new Customer();
        customer.beforeCreate(request, customerRepository);
        customer.create(request);

        assertEquals("Minimal Corp", customer.getCompanyName());
        assertEquals("minimal@example.com", customer.getEmail());
        assertNull(customer.getContactFirstName());
        assertNull(customer.getContactLastName());
        assertNull(customer.getPhone());
        assertNull(customer.getAddressLine1());
    }

    // === UPDATE TESTS ===

    @Test
    void testUpdateCustomerSuccess() {
        // Create a customer first
        Customer customer = new Customer();
        customer.setCompanyName("Original Name");
        customer.setEmail("original@example.com");

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setId(UUID.randomUUID());
        request.setVersion(0L);
        request.setCompanyName("Updated Name");
        request.setEmail("updated@example.com");
        request.setContactFirstName("Jane");
        request.setPhone("555-9999");

        when(customerRepository.existsByEmailAndIsDeletedFalse("updated@example.com"))
                .thenReturn(false);

        customer.beforeUpdate(request, false, customerRepository);
        customer.update(request, false);

        assertEquals("Updated Name", customer.getCompanyName());
        assertEquals("updated@example.com", customer.getEmail());
        assertEquals("Jane", customer.getContactFirstName());
        assertEquals("555-9999", customer.getPhone());
    }

    @Test
    void testBeforeUpdateThrowsExceptionWhenCompanyNameBlank() {
        Customer customer = new Customer();
        customer.setEmail("test@example.com");

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setCompanyName("");
        request.setEmail("test@example.com");

        assertThrows(ValidationException.class, () ->
            customer.beforeUpdate(request, false, customerRepository));
    }

    @Test
    void testBeforeUpdateThrowsExceptionWhenEmailBlank() {
        Customer customer = new Customer();
        customer.setCompanyName("Test Corp");

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setCompanyName("Test Corp");
        request.setEmail("");

        assertThrows(ValidationException.class, () ->
            customer.beforeUpdate(request, false, customerRepository));
    }

    @Test
    void testBeforeUpdateThrowsExceptionWhenChangingToExistingEmail() {
        Customer customer = new Customer();
        customer.setCompanyName("Test Corp");
        customer.setEmail("original@example.com");

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setCompanyName("Test Corp");
        request.setEmail("existing@example.com");

        when(customerRepository.existsByEmailAndIsDeletedFalse("existing@example.com"))
                .thenReturn(true);

        ValidationException exception = assertThrows(ValidationException.class, () ->
            customer.beforeUpdate(request, false, customerRepository));

        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void testBeforeUpdateAllowsSameEmail() {
        Customer customer = new Customer();
        customer.setCompanyName("Test Corp");
        customer.setEmail("same@example.com");

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setCompanyName("Test Corp Updated");
        request.setEmail("same@example.com");

        // Should not throw exception even if email exists (it's the same customer)
        assertDoesNotThrow(() ->
            customer.beforeUpdate(request, false, customerRepository));
    }

    @Test
    void testBeforeUpdateThrowsExceptionWhenUserTriesToUpdateReadOnlyFields() {
        Customer customer = new Customer();
        customer.setCompanyName("Test Corp");
        customer.setEmail("test@example.com");

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setCompanyName("Test Corp");
        request.setEmail("test@example.com");
        request.setDraftInvoiceCount(5);

        ValidationException exception = assertThrows(ValidationException.class, () ->
            customer.beforeUpdate(request, false, customerRepository));

        assertTrue(exception.getMessage().contains("system-managed fields"));
    }

    @Test
    void testUpdateAllowsSystemToUpdateReadOnlyFields() {
        Customer customer = new Customer();
        customer.setCompanyName("Test Corp");
        customer.setEmail("test@example.com");
        customer.setDraftInvoiceCount(0);
        customer.setTotalOutstanding(BigDecimal.ZERO);

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setCompanyName("Test Corp");
        request.setEmail("test@example.com");
        request.setDraftInvoiceCount(5);
        request.setTotalOutstanding(new BigDecimal("1500.00"));

        // System update should not throw
        assertDoesNotThrow(() ->
            customer.beforeUpdate(request, true, customerRepository));

        customer.update(request, true);

        assertEquals(5, customer.getDraftInvoiceCount());
        assertEquals(new BigDecimal("1500.00"), customer.getTotalOutstanding());
    }

    // === HELPER METHOD TESTS ===

    @Test
    void testIncrementAndDecrementInvoiceCounts() {
        Customer customer = new Customer();
        assertEquals(0, customer.getDraftInvoiceCount());
        assertEquals(0, customer.getSentInvoiceCount());
        assertEquals(0, customer.getPaidInvoiceCount());

        customer.incrementDraftInvoiceCount();
        assertEquals(1, customer.getDraftInvoiceCount());

        customer.incrementSentInvoiceCount();
        assertEquals(1, customer.getSentInvoiceCount());

        customer.incrementPaidInvoiceCount();
        assertEquals(1, customer.getPaidInvoiceCount());

        customer.decrementDraftInvoiceCount();
        assertEquals(0, customer.getDraftInvoiceCount());

        customer.decrementSentInvoiceCount();
        assertEquals(0, customer.getSentInvoiceCount());

        customer.decrementPaidInvoiceCount();
        assertEquals(0, customer.getPaidInvoiceCount());
    }

    @Test
    void testAddAndSubtractTotalOutstanding() {
        Customer customer = new Customer();
        assertEquals(BigDecimal.ZERO, customer.getTotalOutstanding());

        customer.addToTotalOutstanding(new BigDecimal("100.50"));
        assertEquals(new BigDecimal("100.50"), customer.getTotalOutstanding());

        customer.addToTotalOutstanding(new BigDecimal("50.25"));
        assertEquals(new BigDecimal("150.75"), customer.getTotalOutstanding());

        customer.subtractFromTotalOutstanding(new BigDecimal("25.00"));
        assertEquals(new BigDecimal("125.75"), customer.getTotalOutstanding());

        customer.subtractFromTotalOutstanding(new BigDecimal("125.75"));
        assertEquals(0, customer.getTotalOutstanding().compareTo(BigDecimal.ZERO));
    }
}
