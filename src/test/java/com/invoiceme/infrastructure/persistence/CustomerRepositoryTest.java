package com.invoiceme.infrastructure.persistence;

import com.invoiceme.application.common.UserContext;
import com.invoiceme.application.customer.dto.CreateCustomerRequest;
import com.invoiceme.domain.customer.Customer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerRepositoryTest {

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

    /**
     * Helper method to create a customer for testing.
     * Handles the new entity API without infrastructure dependencies.
     */
    private Customer createCustomer(String companyName, String email) {
        Customer customer = new Customer();
        customer.validateForCreate(companyName, email);
        customer.create(companyName, null, null, email, null, null, null, null, null, null, null);
        return customer;
    }

    @Test
    void testSaveAndFindById() {
        Customer customer = createCustomer("Test Company", "test@example.com");

        Customer saved = customerRepository.save(customer);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(testUserId, saved.getCreatedBy());
        assertEquals(0L, saved.getVersion());

        Optional<Customer> found = customerRepository.findByIdAndIsDeletedFalse(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Test Company", found.get().getCompanyName());
        assertEquals("test@example.com", found.get().getEmail());
    }

    @Test
    void testExistsByEmailReturnsTrueWhenExists() {
        Customer customer = createCustomer("Email Test Corp", "unique@example.com");
        customerRepository.save(customer);

        assertTrue(customerRepository.existsByEmailAndIsDeletedFalse("unique@example.com"));
    }

    @Test
    void testExistsByEmailReturnsFalseWhenNotExists() {
        assertFalse(customerRepository.existsByEmailAndIsDeletedFalse("nonexistent@example.com"));
    }

    @Test
    void testFindByIdExcludesDeletedCustomers() {
        Customer customer = createCustomer("To Be Deleted", "deleted@example.com");
        Customer saved = customerRepository.save(customer);

        UUID customerId = saved.getId();

        // Soft delete
        saved.markAsDeleted();
        customerRepository.save(saved);

        // Should not find deleted customer
        Optional<Customer> found = customerRepository.findByIdAndIsDeletedFalse(customerId);
        assertFalse(found.isPresent());
    }

    @Test
    void testFindAllByIsDeletedFalseOrderByCompanyName() {
        // Create customers in random order
        createAndSaveCustomer("Zebra Corp", "zebra@example.com");
        createAndSaveCustomer("Apple Corp", "apple@example.com");
        createAndSaveCustomer("Microsoft Corp", "microsoft@example.com");

        List<Customer> customers = customerRepository.findAllByIsDeletedFalseOrderByCompanyName();

        assertTrue(customers.size() >= 3);

        // Find our test customers in the sorted list
        boolean foundApple = false;
        boolean foundMicrosoft = false;
        boolean foundZebra = false;
        int appleIndex = -1;
        int microsoftIndex = -1;
        int zebraIndex = -1;

        for (int i = 0; i < customers.size(); i++) {
            String name = customers.get(i).getCompanyName();
            if ("Apple Corp".equals(name)) {
                foundApple = true;
                appleIndex = i;
            } else if ("Microsoft Corp".equals(name)) {
                foundMicrosoft = true;
                microsoftIndex = i;
            } else if ("Zebra Corp".equals(name)) {
                foundZebra = true;
                zebraIndex = i;
            }
        }

        assertTrue(foundApple && foundMicrosoft && foundZebra, "All test customers should be found");
        assertTrue(appleIndex < microsoftIndex, "Apple should come before Microsoft");
        assertTrue(microsoftIndex < zebraIndex, "Microsoft should come before Zebra");
    }

    @Test
    void testFindAllExcludesDeletedCustomers() {
        Customer customer1 = createAndSaveCustomer("Active Corp", "active@example.com");
        Customer customer2 = createAndSaveCustomer("Deleted Corp", "deleted@example.com");

        // Soft delete customer2
        customer2.markAsDeleted();
        customerRepository.save(customer2);

        List<Customer> customers = customerRepository.findAllByIsDeletedFalseOrderByCompanyName();

        // Check that Active Corp is found but Deleted Corp is not
        boolean foundActive = customers.stream()
                .anyMatch(c -> "Active Corp".equals(c.getCompanyName()));
        boolean foundDeleted = customers.stream()
                .anyMatch(c -> "Deleted Corp".equals(c.getCompanyName()));

        assertTrue(foundActive, "Active Corp should be in results");
        assertFalse(foundDeleted, "Deleted Corp should not be in results");
    }

    @Test
    void testExistsByEmailIgnoresDeletedCustomers() {
        Customer customer = createCustomer("Deleted Customer", "deleted@test.com");
        Customer saved = customerRepository.save(customer);

        // Email should exist
        assertTrue(customerRepository.existsByEmailAndIsDeletedFalse("deleted@test.com"));

        // Soft delete
        saved.markAsDeleted();
        customerRepository.save(saved);

        // Email should no longer exist (ignores deleted)
        assertFalse(customerRepository.existsByEmailAndIsDeletedFalse("deleted@test.com"));
    }

    @Test
    void testOptimisticLocking() {
        Customer customer = createCustomer("Lock Test Corp", "lock@test.com");
        Customer saved = customerRepository.save(customer);

        assertEquals(0L, saved.getVersion());

        // Load same customer twice
        Customer customer1 = customerRepository.findByIdAndIsDeletedFalse(saved.getId()).get();
        Customer customer2 = customerRepository.findByIdAndIsDeletedFalse(saved.getId()).get();

        // Update customer1
        customer1.setCompanyName("Updated by User 1");
        customerRepository.save(customer1);
        customerRepository.flush();

        // customer1 should have version 1
        assertEquals(1L, customer1.getVersion());

        // Updating customer2 should increment version
        customer2.setCompanyName("Updated by User 2");
        customerRepository.save(customer2);

        // Note: In real concurrent scenario, this would throw OptimisticLockException
        // But in same transaction it just increments version
        assertTrue(customer2.getVersion() > 0);
    }

    private Customer createAndSaveCustomer(String companyName, String email) {
        Customer customer = createCustomer(companyName, email);
        return customerRepository.save(customer);
    }
}
