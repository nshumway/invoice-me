package com.invoiceme.application.customer;

import com.invoiceme.application.common.UserContext;
import com.invoiceme.application.customer.dto.*;
import com.invoiceme.domain.common.exceptions.NotFoundException;
import com.invoiceme.domain.common.exceptions.OptimisticLockException;
import com.invoiceme.domain.customer.Customer;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private CustomerService customerService;

    // Use real mapper (not mocked)
    private CustomerMapper customerMapper;

    @BeforeEach
    void setUp() {
        UserContext.setCurrentUser(UUID.randomUUID());
        customerMapper = new CustomerMapper();
        customerService = new CustomerService();
        // Use reflection to inject dependencies
        try {
            var repoField = CustomerService.class.getDeclaredField("customerRepository");
            repoField.setAccessible(true);
            repoField.set(customerService, customerRepository);

            var mapperField = CustomerService.class.getDeclaredField("customerMapper");
            mapperField.setAccessible(true);
            mapperField.set(customerService, customerMapper);

            var eventPublisherField = CustomerService.class.getDeclaredField("eventPublisher");
            eventPublisherField.setAccessible(true);
            eventPublisherField.set(customerService, eventPublisher);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // === CREATE TESTS ===

    @Test
    void testCreateCustomerSuccess() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("Test Corp");
        request.setEmail("test@example.com");

        when(customerRepository.existsByEmailAndIsDeletedFalse("test@example.com"))
                .thenReturn(false);
        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse result = customerService.createCustomer(request);

        assertNotNull(result);
        assertEquals("Test Corp", result.getCompanyName());
        assertEquals("test@example.com", result.getEmail());

        verify(customerRepository).existsByEmailAndIsDeletedFalse("test@example.com");
        verify(customerRepository).save(any(Customer.class));
    }

    // === UPDATE TESTS ===

    @Test
    void testUpdateCustomerSuccess() {
        UUID customerId = UUID.randomUUID();

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setId(customerId);
        request.setVersion(0L);
        request.setCompanyName("Updated Corp");
        request.setEmail("updated@example.com");

        Customer existingCustomer = new Customer();
        existingCustomer.setId(customerId);
        existingCustomer.setCompanyName("Old Corp");
        existingCustomer.setEmail("old@example.com");
        existingCustomer.setVersion(0L);

        when(customerRepository.findByIdAndIsDeletedFalse(customerId))
                .thenReturn(Optional.of(existingCustomer));
        when(customerRepository.existsByEmailAndIsDeletedFalse("updated@example.com"))
                .thenReturn(false);
        when(customerRepository.saveAndFlush(any(Customer.class)))
                .thenAnswer(invocation -> {
                    Customer c = invocation.getArgument(0);
                    // Simulate version increment
                    c.setVersion(c.getVersion() + 1);
                    return c;
                });

        CustomerResponse result = customerService.updateCustomer(request);

        assertNotNull(result);
        assertEquals("Updated Corp", result.getCompanyName());

        verify(customerRepository).findByIdAndIsDeletedFalse(customerId);
        verify(customerRepository).saveAndFlush(any(Customer.class));
    }

    @Test
    void testUpdateCustomerThrowsNotFoundExceptionWhenCustomerDoesNotExist() {
        UUID customerId = UUID.randomUUID();

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setId(customerId);
        request.setVersion(0L);
        request.setCompanyName("Test Corp");
        request.setEmail("test@example.com");

        when(customerRepository.findByIdAndIsDeletedFalse(customerId))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
            customerService.updateCustomer(request));

        verify(customerRepository).findByIdAndIsDeletedFalse(customerId);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void testUpdateCustomerThrowsOptimisticLockExceptionWhenVersionMismatch() {
        UUID customerId = UUID.randomUUID();

        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setId(customerId);
        request.setVersion(0L);  // Client thinks version is 0
        request.setCompanyName("Test Corp");
        request.setEmail("test@example.com");

        Customer existingCustomer = new Customer();
        existingCustomer.setId(customerId);
        existingCustomer.setVersion(1L);  // But database has version 1

        when(customerRepository.findByIdAndIsDeletedFalse(customerId))
                .thenReturn(Optional.of(existingCustomer));

        OptimisticLockException exception = assertThrows(OptimisticLockException.class, () ->
            customerService.updateCustomer(request));

        assertTrue(exception.getMessage().contains("modified by another user"));

        verify(customerRepository).findByIdAndIsDeletedFalse(customerId);
        verify(customerRepository, never()).save(any());
    }

    // === DELETE TESTS ===

    @Test
    void testDeleteCustomerSuccess() {
        UUID customerId = UUID.randomUUID();

        DeleteCustomerRequest request = new DeleteCustomerRequest();
        request.setId(customerId);
        request.setVersion(0L);

        Customer existingCustomer = new Customer();
        existingCustomer.setId(customerId);
        existingCustomer.setVersion(0L);

        when(customerRepository.findByIdAndIsDeletedFalse(customerId))
                .thenReturn(Optional.of(existingCustomer));

        customerService.deleteCustomer(request);

        verify(customerRepository).findByIdAndIsDeletedFalse(customerId);
        verify(customerRepository).save(existingCustomer);
        assertTrue(existingCustomer.getIsDeleted());
    }

    @Test
    void testDeleteCustomerThrowsNotFoundExceptionWhenCustomerDoesNotExist() {
        UUID customerId = UUID.randomUUID();

        DeleteCustomerRequest request = new DeleteCustomerRequest();
        request.setId(customerId);
        request.setVersion(0L);

        when(customerRepository.findByIdAndIsDeletedFalse(customerId))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
            customerService.deleteCustomer(request));

        verify(customerRepository).findByIdAndIsDeletedFalse(customerId);
        verify(customerRepository, never()).save(any());
    }

    @Test
    void testDeleteCustomerThrowsOptimisticLockExceptionWhenVersionMismatch() {
        UUID customerId = UUID.randomUUID();

        DeleteCustomerRequest request = new DeleteCustomerRequest();
        request.setId(customerId);
        request.setVersion(0L);

        Customer existingCustomer = new Customer();
        existingCustomer.setId(customerId);
        existingCustomer.setVersion(1L);  // Version mismatch

        when(customerRepository.findByIdAndIsDeletedFalse(customerId))
                .thenReturn(Optional.of(existingCustomer));

        OptimisticLockException exception = assertThrows(OptimisticLockException.class, () ->
            customerService.deleteCustomer(request));

        assertTrue(exception.getMessage().contains("modified by another user"));

        verify(customerRepository).findByIdAndIsDeletedFalse(customerId);
        verify(customerRepository, never()).save(any());
    }

    // === GET BY ID TESTS ===

    @Test
    void testGetCustomerByIdSuccess() {
        UUID customerId = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setCompanyName("Test Corp");
        customer.setEmail("test@example.com");

        when(customerRepository.findByIdAndIsDeletedFalse(customerId))
                .thenReturn(Optional.of(customer));

        CustomerResponse result = customerService.getCustomerById(customerId);

        assertNotNull(result);
        assertEquals("Test Corp", result.getCompanyName());

        verify(customerRepository).findByIdAndIsDeletedFalse(customerId);
    }

    @Test
    void testGetCustomerByIdThrowsNotFoundExceptionWhenCustomerDoesNotExist() {
        UUID customerId = UUID.randomUUID();

        when(customerRepository.findByIdAndIsDeletedFalse(customerId))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
            customerService.getCustomerById(customerId));

        verify(customerRepository).findByIdAndIsDeletedFalse(customerId);
    }

    // === LIST ALL TESTS ===

    @Test
    void testListAllCustomersSuccess() {
        Customer customer1 = new Customer();
        customer1.setId(UUID.randomUUID());
        customer1.setCompanyName("Acme Corp");
        customer1.setTotalOutstanding(new BigDecimal("1000.00"));

        Customer customer2 = new Customer();
        customer2.setId(UUID.randomUUID());
        customer2.setCompanyName("Beta Inc");
        customer2.setTotalOutstanding(new BigDecimal("500.00"));

        when(customerRepository.findAllByIsDeletedFalseOrderByCompanyName())
                .thenReturn(Arrays.asList(customer1, customer2));

        List<CustomerListItemResponse> results = customerService.listAllCustomers();

        assertEquals(2, results.size());
        assertEquals("Acme Corp", results.get(0).getCompanyName());
        assertEquals("Beta Inc", results.get(1).getCompanyName());

        verify(customerRepository).findAllByIsDeletedFalseOrderByCompanyName();
    }

    @Test
    void testListAllCustomersReturnsEmptyListWhenNoCustomers() {
        when(customerRepository.findAllByIsDeletedFalseOrderByCompanyName())
                .thenReturn(Arrays.asList());

        List<CustomerListItemResponse> results = customerService.listAllCustomers();

        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(customerRepository).findAllByIsDeletedFalseOrderByCompanyName();
    }
}
