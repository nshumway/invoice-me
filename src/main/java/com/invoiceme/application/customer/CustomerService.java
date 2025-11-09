package com.invoiceme.application.customer;

import com.invoiceme.application.customer.dto.*;
import com.invoiceme.domain.common.exceptions.NotFoundException;
import com.invoiceme.domain.common.exceptions.OptimisticLockException;
import com.invoiceme.domain.common.exceptions.ValidationException;
import com.invoiceme.domain.customer.Customer;
import com.invoiceme.domain.customer.events.CustomerDeletedEvent;
import com.invoiceme.domain.customer.events.CustomerNameChangedEvent;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    // === CREATE ===

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        logger.info("Creating customer with email: {}", request.getEmail());
        logger.debug("Create customer request: companyName={}, email={}",
                    request.getCompanyName(), request.getEmail());

        try {
            // Domain validation
            Customer customer = new Customer();
            customer.validateForCreate(request.getCompanyName(), request.getEmail());

            // Infrastructure validation (email uniqueness)
            if (customerRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
                logger.warn("Attempt to create customer with duplicate email: {}", request.getEmail());
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

            logger.info("Successfully created customer: id={}, companyName={}",
                       customer.getId(), customer.getCompanyName());
            return customerMapper.toResponse(customer);
        } catch (ValidationException e) {
            logger.error("Validation failed while creating customer: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error creating customer with email: {}", request.getEmail(), e);
            throw e;
        }
    }

    // === UPDATE (User-initiated) ===

    @Transactional
    public CustomerResponse updateCustomer(UpdateCustomerRequest request) {
        logger.info("Updating customer: id={}", request.getId());
        logger.debug("Update customer request: id={}, companyName={}, version={}",
                    request.getId(), request.getCompanyName(), request.getVersion());

        try {
            // Load entity
            Customer customer = customerRepository.findByIdAndIsDeletedFalse(request.getId())
                    .orElseThrow(() -> {
                        logger.warn("Customer not found for update: id={}", request.getId());
                        return new NotFoundException("Customer not found");
                    });

            // Optimistic locking check
            if (!customer.getVersion().equals(request.getVersion())) {
                logger.warn("Optimistic lock violation for customer: id={}, expected version={}, actual version={}",
                           request.getId(), request.getVersion(), customer.getVersion());
                throw new OptimisticLockException(
                    "Customer was modified by another user. Please reload and try again.");
            }

            // Domain validation
            customer.validateForUpdate(request.getCompanyName(), request.getEmail(), false);

            // Infrastructure validation (email uniqueness if changing)
            if (!request.getEmail().equals(customer.getEmail())) {
                if (customerRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
                    logger.warn("Attempt to update customer with duplicate email: {}", request.getEmail());
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

            // Publish domain event if company name changed (Phase 4-6: Invoice domain will handle this)
            if (!customer.getCompanyName().equals(oldCompanyName)) {
                logger.info("Customer name changed from '{}' to '{}', publishing domain event",
                           oldCompanyName, customer.getCompanyName());
                eventPublisher.publishEvent(
                    new CustomerNameChangedEvent(customer.getId(), oldCompanyName, customer.getCompanyName())
                );
            }

            logger.info("Successfully updated customer: id={}, companyName={}",
                       customer.getId(), customer.getCompanyName());
            return customerMapper.toResponse(saved);
        } catch (NotFoundException | OptimisticLockException | ValidationException e) {
            logger.error("Error updating customer id={}: {}", request.getId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error updating customer: id={}", request.getId(), e);
            throw e;
        }
    }

    // === DELETE ===

    @Transactional
    public void deleteCustomer(DeleteCustomerRequest request) {
        logger.info("Deleting customer: id={}", request.getId());
        logger.debug("Delete customer request: id={}, version={}", request.getId(), request.getVersion());

        try {
            Customer customer = customerRepository.findByIdAndIsDeletedFalse(request.getId())
                    .orElseThrow(() -> {
                        logger.warn("Customer not found for deletion: id={}", request.getId());
                        return new NotFoundException("Customer not found");
                    });

            // Optimistic locking check
            if (!customer.getVersion().equals(request.getVersion())) {
                logger.warn("Optimistic lock violation for customer deletion: id={}, expected version={}, actual version={}",
                           request.getId(), request.getVersion(), customer.getVersion());
                throw new OptimisticLockException(
                    "Customer was modified by another user. Please reload and try again.");
            }

            // TODO (Phase 4-6): Validate customer can be deleted (no SENT invoices)
            // List<Invoice> sentInvoices = customerRepository.getSentInvoicesForCustomer(customer.getId());
            // if (!sentInvoices.isEmpty()) {
            //     logger.warn("Cannot delete customer {} with {} SENT invoice(s)",
            //                customer.getId(), sentInvoices.size());
            //     throw new ValidationException("Cannot delete customer with SENT invoices. " +
            //             "Customer has " + sentInvoices.size() + " invoice(s) in SENT status.");
            // }

            // Perform soft delete
            String companyName = customer.getCompanyName();
            customer.delete();
            customerRepository.save(customer);

            // Publish domain event (Phase 4-6: Invoice domain will handle cascade deletion)
            logger.info("Customer soft deleted, publishing domain event: id={}, companyName={}",
                       customer.getId(), companyName);
            eventPublisher.publishEvent(
                new CustomerDeletedEvent(customer.getId(), companyName)
            );

            logger.info("Successfully deleted customer: id={}, companyName={}", customer.getId(), companyName);
        } catch (NotFoundException | OptimisticLockException e) {
            logger.error("Error deleting customer id={}: {}", request.getId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error deleting customer: id={}", request.getId(), e);
            throw e;
        }
    }

    // === GET BY ID ===

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(UUID id) {
        logger.debug("Retrieving customer by id: {}", id);

        try {
            Customer customer = customerRepository.findByIdAndIsDeletedFalse(id)
                    .orElseThrow(() -> {
                        logger.warn("Customer not found: id={}", id);
                        return new NotFoundException("Customer not found");
                    });

            logger.debug("Successfully retrieved customer: id={}, companyName={}",
                        customer.getId(), customer.getCompanyName());
            return customerMapper.toResponse(customer);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error retrieving customer: id={}", id, e);
            throw e;
        }
    }

    // === LIST ALL ===

    @Transactional(readOnly = true)
    public List<CustomerListItemResponse> listAllCustomers() {
        logger.debug("Listing all customers");

        try {
            List<CustomerListItemResponse> customers = customerRepository.findAllByIsDeletedFalseOrderByCompanyName()
                    .stream()
                    .map(customerMapper::toListItem)
                    .collect(Collectors.toList());

            logger.info("Successfully retrieved {} customer(s)", customers.size());
            return customers;
        } catch (Exception e) {
            logger.error("Unexpected error listing all customers", e);
            throw e;
        }
    }
}
