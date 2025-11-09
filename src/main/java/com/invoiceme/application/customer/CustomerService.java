package com.invoiceme.application.customer;

import com.invoiceme.application.customer.dto.*;
import com.invoiceme.domain.common.exceptions.NotFoundException;
import com.invoiceme.domain.common.exceptions.OptimisticLockException;
import com.invoiceme.domain.customer.Customer;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
import com.invoiceme.application.invoice.InvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private InvoiceService invoiceService;

    // === CREATE ===

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        Customer customer = new Customer();

        customer.beforeCreate(request, customerRepository);
        customer.create(request);
        customerRepository.save(customer);
        customer.afterCreate();

        return customerMapper.toResponse(customer);
    }

    // === UPDATE (User-initiated) ===

    @Transactional
    public CustomerResponse updateCustomer(UpdateCustomerRequest request) {
        return doUpdate(request, false);
    }

    // === UPDATE (System-initiated) ===

    @Transactional(propagation = Propagation.MANDATORY)
    public CustomerResponse systemUpdateCustomer(UpdateCustomerRequest request) {
        return doUpdate(request, true);
    }

    private CustomerResponse doUpdate(UpdateCustomerRequest request, boolean isSystemUpdate) {
        // Load entity
        Customer customer = customerRepository.findByIdAndIsDeletedFalse(request.getId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        // Optimistic locking check
        if (!customer.getVersion().equals(request.getVersion())) {
            throw new OptimisticLockException(
                "Customer was modified by another user. Please reload and try again.");
        }

        // Capture old values for cascading
        String oldCompanyName = customer.getCompanyName();

        // Execute lifecycle
        customer.beforeUpdate(request, isSystemUpdate, customerRepository);
        customer.update(request, isSystemUpdate);
        customerRepository.save(customer);
        customer.afterUpdate(oldCompanyName, customerRepository, invoiceService);

        return customerMapper.toResponse(customer);
    }

    // === DELETE ===

    @Transactional
    public void deleteCustomer(DeleteCustomerRequest request) {
        Customer customer = customerRepository.findByIdAndIsDeletedFalse(request.getId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        // Optimistic locking check
        if (!customer.getVersion().equals(request.getVersion())) {
            throw new OptimisticLockException(
                "Customer was modified by another user. Please reload and try again.");
        }

        customer.beforeDelete(customerRepository);
        customer.delete();
        customerRepository.save(customer);
        customer.afterDelete(customerRepository, invoiceService);
    }

    // === SYSTEM DELETE (Called from cascades) ===

    @Transactional(propagation = Propagation.MANDATORY)
    public void systemDeleteCustomer(UUID customerId) {
        Customer customer = customerRepository.findByIdAndIsDeletedFalse(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        // System deletes skip some validations but still run lifecycle
        customer.delete();
        customerRepository.save(customer);
        customer.afterDelete(customerRepository, invoiceService);
    }

    // === GET BY ID ===

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(UUID id) {
        Customer customer = customerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        return customerMapper.toResponse(customer);
    }

    // === LIST ALL ===

    @Transactional(readOnly = true)
    public List<CustomerListItemResponse> listAllCustomers() {
        return customerRepository.findAllByIsDeletedFalseOrderByCompanyName()
                .stream()
                .map(customerMapper::toListItem)
                .collect(Collectors.toList());
    }
}
