package com.invoiceme.application.customer;

import com.invoiceme.application.customer.dto.*;
import com.invoiceme.domain.common.exceptions.NotFoundException;
import com.invoiceme.domain.common.exceptions.OptimisticLockException;
import com.invoiceme.domain.common.exceptions.ValidationException;
import com.invoiceme.domain.customer.Customer;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
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

    // === CREATE ===

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        // Domain validation
        Customer customer = new Customer();
        customer.validateForCreate(request.getCompanyName(), request.getEmail());

        // Infrastructure validation (email uniqueness)
        if (customerRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
            throw new ValidationException("Customer with email " + request.getEmail() + " already exists");
        }

        // Create customer
        customer.create(
            request.getCompanyName(),
            request.getContactFirstName(),
            request.getContactLastName(),
            request.getEmail(),
            request.getPhone(),
            request.getAddressLine1(),
            request.getAddressLine2(),
            request.getCity(),
            request.getState(),
            request.getZipCode(),
            request.getCountry()
        );

        customerRepository.save(customer);

        return customerMapper.toResponse(customer);
    }

    // === UPDATE (User-initiated) ===

    @Transactional
    public CustomerResponse updateCustomer(UpdateCustomerRequest request) {
        // Load entity
        Customer customer = customerRepository.findByIdAndIsDeletedFalse(request.getId())
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        // Optimistic locking check
        if (!customer.getVersion().equals(request.getVersion())) {
            throw new OptimisticLockException(
                "Customer was modified by another user. Please reload and try again.");
        }

        // Domain validation
        customer.validateForUpdate(request.getCompanyName(), request.getEmail(), false);

        // Infrastructure validation (email uniqueness if changing)
        if (!request.getEmail().equals(customer.getEmail())) {
            if (customerRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
                throw new ValidationException("Customer with email " + request.getEmail() + " already exists");
            }
        }

        // Capture old company name for potential cascade (Phase 4-6)
        String oldCompanyName = customer.getCompanyName();

        // Update customer
        customer.update(
            request.getCompanyName(),
            request.getContactFirstName(),
            request.getContactLastName(),
            request.getEmail(),
            request.getPhone(),
            request.getAddressLine1(),
            request.getAddressLine2(),
            request.getCity(),
            request.getState(),
            request.getZipCode(),
            request.getCountry()
        );

        Customer saved = customerRepository.saveAndFlush(customer);

        // TODO (Phase 4-6): If company name changed, cascade to invoices
        // if (!customer.getCompanyName().equals(oldCompanyName)) {
        //     List<Invoice> invoices = customerRepository.getInvoicesForCustomer(customer.getId());
        //     for (Invoice invoice : invoices) {
        //         invoiceService.updateCustomerName(invoice.getId(), customer.getCompanyName());
        //     }
        // }

        return customerMapper.toResponse(saved);
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

        // TODO (Phase 4-6): Validate customer can be deleted (no SENT invoices)
        // List<Invoice> sentInvoices = customerRepository.getSentInvoicesForCustomer(customer.getId());
        // if (!sentInvoices.isEmpty()) {
        //     throw new ValidationException("Cannot delete customer with SENT invoices. " +
        //             "Customer has " + sentInvoices.size() + " invoice(s) in SENT status.");
        // }

        // Perform soft delete
        customer.delete();
        customerRepository.save(customer);

        // TODO (Phase 4-6): Cascade delete to related invoices (DRAFT and PAID only)
        // List<Invoice> invoices = customerRepository.getInvoicesForCustomer(customer.getId());
        // for (Invoice invoice : invoices) {
        //     invoiceService.deleteInvoice(invoice.getId());
        // }
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
