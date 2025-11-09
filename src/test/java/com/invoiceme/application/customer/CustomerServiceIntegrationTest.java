package com.invoiceme.application.customer;

import com.invoiceme.application.common.UserContext;
import com.invoiceme.application.customer.dto.*;
import com.invoiceme.domain.common.exceptions.NotFoundException;
import com.invoiceme.domain.common.exceptions.OptimisticLockException;
import com.invoiceme.domain.common.exceptions.ValidationException;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerServiceIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

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
    void testCreateCustomerIntegration() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("Integration Test Corp");
        request.setEmail("integration@test.com");
        request.setContactFirstName("John");
        request.setContactLastName("Doe");
        request.setPhone("555-1234");
        request.setAddressLine1("123 Main St");
        request.setCity("Boston");
        request.setState("MA");
        request.setZipCode("02101");
        request.setCountry("USA");

        CustomerResponse response = customerService.createCustomer(request);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("Integration Test Corp", response.getCompanyName());
        assertEquals("integration@test.com", response.getEmail());
        assertEquals("John", response.getContactFirstName());
        assertEquals("Doe", response.getContactLastName());
        assertEquals("555-1234", response.getPhone());
        assertEquals("123 Main St", response.getAddressLine1());
        assertEquals("Boston", response.getCity());
        assertEquals("MA", response.getState());
        assertEquals("02101", response.getZipCode());
        assertEquals("USA", response.getCountry());
        assertEquals(0, response.getDraftInvoiceCount());
        assertEquals(0, response.getSentInvoiceCount());
        assertEquals(0, response.getPaidInvoiceCount());
        assertEquals(BigDecimal.ZERO, response.getTotalOutstanding());
        // Note: createdAt, createdBy, and version are managed by JPA and may not
        // be populated until the transaction is committed and entity is retrieved
        assertEquals(0L, response.getVersion());
    }

    @Test
    void testCreateCustomerWithDuplicateEmailThrowsException() {
        CreateCustomerRequest request1 = new CreateCustomerRequest();
        request1.setCompanyName("First Corp");
        request1.setEmail("duplicate@test.com");

        customerService.createCustomer(request1);

        CreateCustomerRequest request2 = new CreateCustomerRequest();
        request2.setCompanyName("Second Corp");
        request2.setEmail("duplicate@test.com");

        ValidationException exception = assertThrows(ValidationException.class, () ->
            customerService.createCustomer(request2));

        assertTrue(exception.getMessage().contains("already exists"));
    }

    // === UPDATE TESTS ===

    @Test
    void testUpdateCustomerIntegration() {
        // Create customer
        CreateCustomerRequest createRequest = new CreateCustomerRequest();
        createRequest.setCompanyName("Original Name");
        createRequest.setEmail("original@test.com");

        CustomerResponse created = customerService.createCustomer(createRequest);

        // Update customer
        UpdateCustomerRequest updateRequest = new UpdateCustomerRequest();
        updateRequest.setId(created.getId());
        updateRequest.setVersion(created.getVersion());
        updateRequest.setCompanyName("Updated Name");
        updateRequest.setEmail("updated@test.com");
        updateRequest.setContactFirstName("Jane");
        updateRequest.setPhone("555-9999");

        CustomerResponse updated = customerService.updateCustomer(updateRequest);

        assertEquals(created.getId(), updated.getId());
        assertEquals("Updated Name", updated.getCompanyName());
        assertEquals("updated@test.com", updated.getEmail());
        assertEquals("Jane", updated.getContactFirstName());
        assertEquals("555-9999", updated.getPhone());
        assertEquals(1L, updated.getVersion());  // Version incremented
    }

    @Test
    void testUpdateCustomerWithWrongVersionThrowsOptimisticLockException() {
        // Create customer
        CreateCustomerRequest createRequest = new CreateCustomerRequest();
        createRequest.setCompanyName("Lock Test");
        createRequest.setEmail("lock@test.com");

        CustomerResponse created = customerService.createCustomer(createRequest);

        // Try to update with wrong version
        UpdateCustomerRequest updateRequest = new UpdateCustomerRequest();
        updateRequest.setId(created.getId());
        updateRequest.setVersion(999L);  // Wrong version
        updateRequest.setCompanyName("Should Fail");
        updateRequest.setEmail("lock@test.com");

        assertThrows(OptimisticLockException.class, () ->
            customerService.updateCustomer(updateRequest));
    }

    @Test
    void testUpdateNonExistentCustomerThrowsNotFoundException() {
        UpdateCustomerRequest updateRequest = new UpdateCustomerRequest();
        updateRequest.setId(UUID.randomUUID());  // Non-existent ID
        updateRequest.setVersion(0L);
        updateRequest.setCompanyName("Test");
        updateRequest.setEmail("test@test.com");

        assertThrows(NotFoundException.class, () ->
            customerService.updateCustomer(updateRequest));
    }

    // === DELETE TESTS ===

    @Test
    void testDeleteCustomerIntegration() {
        // Create customer
        CreateCustomerRequest createRequest = new CreateCustomerRequest();
        createRequest.setCompanyName("To Be Deleted");
        createRequest.setEmail("delete@test.com");

        CustomerResponse created = customerService.createCustomer(createRequest);

        // Delete customer
        DeleteCustomerRequest deleteRequest = new DeleteCustomerRequest();
        deleteRequest.setId(created.getId());
        deleteRequest.setVersion(created.getVersion());

        customerService.deleteCustomer(deleteRequest);

        // Verify customer is soft deleted
        assertThrows(NotFoundException.class, () ->
            customerService.getCustomerById(created.getId()));
    }

    @Test
    void testDeleteCustomerWithWrongVersionThrowsOptimisticLockException() {
        // Create customer
        CreateCustomerRequest createRequest = new CreateCustomerRequest();
        createRequest.setCompanyName("Delete Lock Test");
        createRequest.setEmail("deletelock@test.com");

        CustomerResponse created = customerService.createCustomer(createRequest);

        // Try to delete with wrong version
        DeleteCustomerRequest deleteRequest = new DeleteCustomerRequest();
        deleteRequest.setId(created.getId());
        deleteRequest.setVersion(999L);  // Wrong version

        assertThrows(OptimisticLockException.class, () ->
            customerService.deleteCustomer(deleteRequest));
    }

    // === GET BY ID TESTS ===

    @Test
    void testGetCustomerByIdIntegration() {
        // Create customer
        CreateCustomerRequest createRequest = new CreateCustomerRequest();
        createRequest.setCompanyName("Get Test Corp");
        createRequest.setEmail("get@test.com");

        CustomerResponse created = customerService.createCustomer(createRequest);

        // Get customer
        CustomerResponse retrieved = customerService.getCustomerById(created.getId());

        assertEquals(created.getId(), retrieved.getId());
        assertEquals("Get Test Corp", retrieved.getCompanyName());
        assertEquals("get@test.com", retrieved.getEmail());
    }

    @Test
    void testGetNonExistentCustomerThrowsNotFoundException() {
        assertThrows(NotFoundException.class, () ->
            customerService.getCustomerById(UUID.randomUUID()));
    }

    // === LIST ALL TESTS ===

    @Test
    void testListAllCustomersIntegration() {
        // Create multiple customers
        createCustomer("Alpha Corp", "alpha@test.com");
        createCustomer("Beta Corp", "beta@test.com");
        createCustomer("Gamma Corp", "gamma@test.com");

        List<CustomerListItemResponse> customers = customerService.listAllCustomers();

        assertTrue(customers.size() >= 3);

        // Verify our customers are in the list
        boolean foundAlpha = customers.stream()
                .anyMatch(c -> "Alpha Corp".equals(c.getCompanyName()));
        boolean foundBeta = customers.stream()
                .anyMatch(c -> "Beta Corp".equals(c.getCompanyName()));
        boolean foundGamma = customers.stream()
                .anyMatch(c -> "Gamma Corp".equals(c.getCompanyName()));

        assertTrue(foundAlpha);
        assertTrue(foundBeta);
        assertTrue(foundGamma);
    }

    @Test
    void testListAllExcludesDeletedCustomers() {
        // Create two customers
        CustomerResponse customer1 = createCustomer("Active Corp", "active@test.com");
        CustomerResponse customer2 = createCustomer("To Delete Corp", "todelete@test.com");

        // Delete second customer
        DeleteCustomerRequest deleteRequest = new DeleteCustomerRequest();
        deleteRequest.setId(customer2.getId());
        deleteRequest.setVersion(customer2.getVersion());
        customerService.deleteCustomer(deleteRequest);

        // List all customers
        List<CustomerListItemResponse> customers = customerService.listAllCustomers();

        // Check that Active Corp is found but To Delete Corp is not
        boolean foundActive = customers.stream()
                .anyMatch(c -> "Active Corp".equals(c.getCompanyName()));
        boolean foundDeleted = customers.stream()
                .anyMatch(c -> "To Delete Corp".equals(c.getCompanyName()));

        assertTrue(foundActive);
        assertFalse(foundDeleted);
    }

    // === HELPER METHODS ===

    private CustomerResponse createCustomer(String companyName, String email) {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName(companyName);
        request.setEmail(email);
        return customerService.createCustomer(request);
    }
}
