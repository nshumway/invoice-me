# InvoiceMe - Customer Implementation

## Table of Contents
- [Overview](#overview)
- [Customer Entity](#customer-entity)
- [DTOs (Data Transfer Objects)](#dtos-data-transfer-objects)
- [Customer Repository](#customer-repository)
- [Customer Mapper](#customer-mapper)
- [Customer Service](#customer-service)
- [Customer Controller](#customer-controller)
- [Complete Transaction Walkthrough](#complete-transaction-walkthrough)
- [Validation Rules](#validation-rules)
- [Testing Strategy](#testing-strategy)

---

## Overview

The Customer entity represents clients/companies in the invoicing system. Customers can have multiple invoices, and the system tracks aggregate statistics like total outstanding balance and invoice counts.

**Operations:**
- `CreateCustomer` - Create a new customer
- `UpdateCustomer` - Update customer details (cascades customerName to related invoices)
- `DeleteCustomer` - Soft delete customer (with validation)
- `GetCustomerById` - Retrieve single customer by ID
- `ListAllCustomers` - List customers with filtering and pagination

---

## Customer Entity

### Customer.java
```java
package com.invoiceme.domain.customer;

import com.invoiceme.domain.common.BaseEntity;
import com.invoiceme.domain.common.exceptions.ValidationException;
import com.invoiceme.application.customer.dto.*;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
import com.invoiceme.application.invoice.InvoiceService;
import com.invoiceme.application.invoice.dto.UpdateInvoiceRequest;
import com.invoiceme.domain.invoice.Invoice;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

    // === Fields ===

    @Column(nullable = false)
    private String companyName;

    private String contactFirstName;
    private String contactLastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    // === Read-Only Fields (System Managed) ===

    @Column(nullable = false)
    private Integer draftInvoiceCount = 0;

    @Column(nullable = false)
    private Integer sentInvoiceCount = 0;

    @Column(nullable = false)
    private Integer paidInvoiceCount = 0;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalOutstanding = BigDecimal.ZERO;

    // === Constructors ===

    public Customer() {
    }

    // === CREATE OPERATION ===

    public void beforeCreate(CreateCustomerRequest request, CustomerRepository customerRepository) {
        // Validate company name
        if (request.getCompanyName() == null || request.getCompanyName().isBlank()) {
            throw new ValidationException("Company name is required");
        }

        // Validate email required
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ValidationException("Email is required");
        }

        // Validate email uniqueness
        if (customerRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
            throw new ValidationException("Customer with email " + request.getEmail() + " already exists");
        }
    }

    public void create(CreateCustomerRequest request) {
        this.companyName = request.getCompanyName();
        this.contactFirstName = request.getContactFirstName();
        this.contactLastName = request.getContactLastName();
        this.email = request.getEmail();
        this.phone = request.getPhone();
        this.addressLine1 = request.getAddressLine1();
        this.addressLine2 = request.getAddressLine2();
        this.city = request.getCity();
        this.state = request.getState();
        this.zipCode = request.getZipCode();
        this.country = request.getCountry();

        // Initialize read-only fields
        this.draftInvoiceCount = 0;
        this.sentInvoiceCount = 0;
        this.paidInvoiceCount = 0;
        this.totalOutstanding = BigDecimal.ZERO;
    }

    public void afterCreate() {
        // No cascading operations needed for create
    }

    // === UPDATE OPERATION ===

    public void beforeUpdate(UpdateCustomerRequest request, boolean isSystemUpdate,
                            CustomerRepository customerRepository) {
        // Validate company name
        if (request.getCompanyName() == null || request.getCompanyName().isBlank()) {
            throw new ValidationException("Company name is required");
        }

        // Validate email required
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new ValidationException("Email is required");
        }

        // Validate email uniqueness (if changing)
        if (!request.getEmail().equals(this.email)) {
            if (customerRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
                throw new ValidationException("Customer with email " + request.getEmail() + " already exists");
            }
        }

        // Prevent user from updating read-only fields
        if (!isSystemUpdate) {
            if (request.getDraftInvoiceCount() != null ||
                request.getSentInvoiceCount() != null ||
                request.getPaidInvoiceCount() != null ||
                request.getTotalOutstanding() != null) {
                throw new ValidationException("Cannot update system-managed fields");
            }
        }
    }

    public void update(UpdateCustomerRequest request, boolean isSystemUpdate) {
        // Update writable fields
        if (request.getCompanyName() != null) {
            this.companyName = request.getCompanyName();
        }
        if (request.getContactFirstName() != null) {
            this.contactFirstName = request.getContactFirstName();
        }
        if (request.getContactLastName() != null) {
            this.contactLastName = request.getContactLastName();
        }
        if (request.getEmail() != null) {
            this.email = request.getEmail();
        }
        if (request.getPhone() != null) {
            this.phone = request.getPhone();
        }
        if (request.getAddressLine1() != null) {
            this.addressLine1 = request.getAddressLine1();
        }
        if (request.getAddressLine2() != null) {
            this.addressLine2 = request.getAddressLine2();
        }
        if (request.getCity() != null) {
            this.city = request.getCity();
        }
        if (request.getState() != null) {
            this.state = request.getState();
        }
        if (request.getZipCode() != null) {
            this.zipCode = request.getZipCode();
        }
        if (request.getCountry() != null) {
            this.country = request.getCountry();
        }

        // System can update read-only fields
        if (isSystemUpdate) {
            if (request.getDraftInvoiceCount() != null) {
                this.draftInvoiceCount = request.getDraftInvoiceCount();
            }
            if (request.getSentInvoiceCount() != null) {
                this.sentInvoiceCount = request.getSentInvoiceCount();
            }
            if (request.getPaidInvoiceCount() != null) {
                this.paidInvoiceCount = request.getPaidInvoiceCount();
            }
            if (request.getTotalOutstanding() != null) {
                this.totalOutstanding = request.getTotalOutstanding();
            }
        }
    }

    public void afterUpdate(String oldCompanyName,
                           CustomerRepository customerRepository,
                           InvoiceService invoiceService) {
        // Cascade companyName change to all related invoices
        if (!this.companyName.equals(oldCompanyName)) {
            List<Invoice> invoices = customerRepository.getInvoicesForCustomer(this.getId());

            for (Invoice invoice : invoices) {
                // Create system update request
                UpdateInvoiceRequest sysRequest = new UpdateInvoiceRequest();
                sysRequest.setId(invoice.getId());
                sysRequest.setVersion(invoice.getVersion());
                sysRequest.setCustomerName(this.companyName);

                // Call invoice service (joins same transaction, cascades to LineItems/Payments)
                invoiceService.systemUpdateInvoice(sysRequest);
            }
        }
    }

    // === DELETE OPERATION ===

    public void beforeDelete(CustomerRepository customerRepository) {
        // Cannot delete if customer has SENT invoices
        List<Invoice> sentInvoices = customerRepository.getSentInvoicesForCustomer(this.getId());
        if (!sentInvoices.isEmpty()) {
            throw new ValidationException("Cannot delete customer with SENT invoices. " +
                    "Customer has " + sentInvoices.size() + " invoice(s) in SENT status.");
        }
    }

    public void delete() {
        this.markAsDeleted(); // Soft delete from BaseEntity
    }

    public void afterDelete(CustomerRepository customerRepository, InvoiceService invoiceService) {
        // Cascade delete to all related invoices (DRAFT and PAID only, SENT already validated)
        List<Invoice> invoices = customerRepository.getInvoicesForCustomer(this.getId());

        for (Invoice invoice : invoices) {
            // Each invoice delete will cascade to its LineItems and Payments
            invoiceService.systemDeleteInvoice(invoice.getId());
        }
    }

    // === Helper Methods ===

    public void incrementDraftInvoiceCount() {
        this.draftInvoiceCount++;
    }

    public void decrementDraftInvoiceCount() {
        this.draftInvoiceCount--;
    }

    public void incrementSentInvoiceCount() {
        this.sentInvoiceCount++;
    }

    public void decrementSentInvoiceCount() {
        this.sentInvoiceCount--;
    }

    public void incrementPaidInvoiceCount() {
        this.paidInvoiceCount++;
    }

    public void decrementPaidInvoiceCount() {
        this.paidInvoiceCount--;
    }

    public void addToTotalOutstanding(BigDecimal amount) {
        this.totalOutstanding = this.totalOutstanding.add(amount);
    }

    public void subtractFromTotalOutstanding(BigDecimal amount) {
        this.totalOutstanding = this.totalOutstanding.subtract(amount);
    }

    // === Getters and Setters ===
    // (All standard getters/setters omitted for brevity)

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Integer getDraftInvoiceCount() { return draftInvoiceCount; }
    public Integer getSentInvoiceCount() { return sentInvoiceCount; }
    public Integer getPaidInvoiceCount() { return paidInvoiceCount; }
    public BigDecimal getTotalOutstanding() { return totalOutstanding; }

    // ... all other getters/setters
}
```

---

## DTOs (Data Transfer Objects)

### CreateCustomerRequest.java
```java
package com.invoiceme.application.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CreateCustomerRequest {

    @NotBlank(message = "Company name is required")
    private String companyName;

    private String contactFirstName;
    private String contactLastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    // === Getters and Setters ===
    // (Standard getters/setters for all fields)
}
```

### UpdateCustomerRequest.java
```java
package com.invoiceme.application.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public class UpdateCustomerRequest {

    @NotNull(message = "Customer ID is required")
    private UUID id;

    @NotNull(message = "Version is required for optimistic locking")
    private Long version;

    @NotBlank(message = "Company name is required")
    private String companyName;

    private String contactFirstName;
    private String contactLastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    // === Read-only fields (only for system updates) ===
    private Integer draftInvoiceCount;
    private Integer sentInvoiceCount;
    private Integer paidInvoiceCount;
    private BigDecimal totalOutstanding;

    // === Getters and Setters ===
}
```

### DeleteCustomerRequest.java
```java
package com.invoiceme.application.customer.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class DeleteCustomerRequest {

    @NotNull(message = "Customer ID is required")
    private UUID id;

    @NotNull(message = "Version is required for optimistic locking")
    private Long version;

    // === Getters and Setters ===
}
```

### CustomerResponse.java
```java
package com.invoiceme.application.customer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CustomerResponse {

    // Entity ID and audit fields
    private UUID id;
    private Instant createdAt;
    private UUID createdBy;
    private Instant lastModifiedAt;
    private UUID lastModifiedBy;
    private Long version;

    // Customer fields
    private String companyName;
    private String contactFirstName;
    private String contactLastName;
    private String email;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    // Read-only computed fields
    private Integer draftInvoiceCount;
    private Integer sentInvoiceCount;
    private Integer paidInvoiceCount;
    private BigDecimal totalOutstanding;

    // === Getters and Setters ===
}
```

### CustomerListItemResponse.java
```java
package com.invoiceme.application.customer.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lightweight DTO for customer lists - only includes essential fields
 */
public class CustomerListItemResponse {

    private UUID id;
    private String companyName;
    private String email;
    private BigDecimal totalOutstanding;

    // === Getters and Setters ===
}
```

---

## Customer Repository

### CustomerRepository.java
```java
package com.invoiceme.infrastructure.persistence;

import com.invoiceme.domain.customer.Customer;
import com.invoiceme.domain.invoice.Invoice;
import com.invoiceme.domain.invoice.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    // === Standard Queries ===

    Optional<Customer> findByIdAndIsDeletedFalse(UUID id);

    List<Customer> findAllByIsDeletedFalseOrderByCompanyName();

    boolean existsByEmailAndIsDeletedFalse(String email);

    // === Custom Queries for Related Entities ===

    /**
     * Get all invoices for a customer (used for cascading updates)
     */
    @Query("SELECT i FROM Invoice i WHERE i.customerId = :customerId AND i.isDeleted = false")
    List<Invoice> getInvoicesForCustomer(@Param("customerId") UUID customerId);

    /**
     * Get only SENT invoices for a customer (used for delete validation)
     */
    @Query("SELECT i FROM Invoice i WHERE i.customerId = :customerId " +
           "AND i.status = :status AND i.isDeleted = false")
    List<Invoice> getSentInvoicesForCustomer(@Param("customerId") UUID customerId);

    // Note: This would actually use InvoiceStatus.SENT but showing for clarity
    default List<Invoice> getSentInvoicesForCustomer(UUID customerId) {
        return getSentInvoicesForCustomerByStatus(customerId, InvoiceStatus.SENT);
    }

    @Query("SELECT i FROM Invoice i WHERE i.customerId = :customerId " +
           "AND i.status = :status AND i.isDeleted = false")
    List<Invoice> getSentInvoicesForCustomerByStatus(@Param("customerId") UUID customerId,
                                                      @Param("status") InvoiceStatus status);
}
```

---

## Customer Mapper

### CustomerMapper.java
```java
package com.invoiceme.application.customer;

import com.invoiceme.application.customer.dto.*;
import com.invoiceme.domain.customer.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer entity) {
        CustomerResponse dto = new CustomerResponse();

        // BaseEntity fields
        dto.setId(entity.getId());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setLastModifiedAt(entity.getLastModifiedAt());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setVersion(entity.getVersion());

        // Customer fields
        dto.setCompanyName(entity.getCompanyName());
        dto.setContactFirstName(entity.getContactFirstName());
        dto.setContactLastName(entity.getContactLastName());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setAddressLine1(entity.getAddressLine1());
        dto.setAddressLine2(entity.getAddressLine2());
        dto.setCity(entity.getCity());
        dto.setState(entity.getState());
        dto.setZipCode(entity.getZipCode());
        dto.setCountry(entity.getCountry());

        // Read-only computed fields
        dto.setDraftInvoiceCount(entity.getDraftInvoiceCount());
        dto.setSentInvoiceCount(entity.getSentInvoiceCount());
        dto.setPaidInvoiceCount(entity.getPaidInvoiceCount());
        dto.setTotalOutstanding(entity.getTotalOutstanding());

        return dto;
    }

    public CustomerListItemResponse toListItem(Customer entity) {
        CustomerListItemResponse dto = new CustomerListItemResponse();

        dto.setId(entity.getId());
        dto.setCompanyName(entity.getCompanyName());
        dto.setEmail(entity.getEmail());
        dto.setTotalOutstanding(entity.getTotalOutstanding());

        return dto;
    }
}
```

---

## Customer Service

### CustomerService.java
```java
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
```

---

## Customer Controller

### CustomerController.java
```java
package com.invoiceme.infrastructure.web;

import com.invoiceme.application.common.ApiResponse;
import com.invoiceme.application.customer.CustomerService;
import com.invoiceme.application.customer.dto.*;
import com.invoiceme.domain.common.exceptions.NotFoundException;
import com.invoiceme.domain.common.exceptions.OptimisticLockException;
import com.invoiceme.domain.common.exceptions.ValidationException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    // === CREATE ===

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {

        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created successfully", response));
    }

    // === UPDATE ===

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request) {

        request.setId(id); // Ensure ID from path
        CustomerResponse response = customerService.updateCustomer(request);
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", response));
    }

    // === DELETE ===

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(
            @PathVariable UUID id,
            @RequestParam Long version) {

        DeleteCustomerRequest request = new DeleteCustomerRequest();
        request.setId(id);
        request.setVersion(version);

        customerService.deleteCustomer(request);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully", null));
    }

    // === GET BY ID ===

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable UUID id) {
        CustomerResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // === LIST ALL ===

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerListItemResponse>>> listAllCustomers() {
        List<CustomerListItemResponse> response = customerService.listAllCustomers();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

---

## Complete Transaction Walkthrough

### Scenario: Update Customer Company Name

**Initial State:**
- Customer "Acme Corp" exists with ID `123e4567-e89b-12d3-a456-426614174000`
- Customer has 2 invoices:
  - Invoice #1 (DRAFT) with 3 line items
  - Invoice #2 (SENT) with 2 line items and 1 payment
- User wants to change company name to "Acme Corporation"

**HTTP Request:**
```http
PUT /api/customers/123e4567-e89b-12d3-a456-426614174000
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "version": 5,
  "companyName": "Acme Corporation",
  "email": "billing@acme.com"
}
```

**Step-by-Step Execution:**

```
1. HTTP Request arrives at CustomerController

2. Spring Security extracts JWT token
   → JwtAuthenticationFilter.doFilterInternal()
   → Extracts userId from token
   → Sets SecurityContext authentication
   → UserContext.setCurrentUser(userId)

3. Controller receives request
   → Bean Validation runs (@Valid)
   → Validates @NotBlank on companyName, @Email on email
   → request.setId(id) from path parameter

4. Controller calls CustomerService.updateCustomer(request)

5. @Transactional boundary starts
   ↓

6. Service loads Customer from database
   → customerRepository.findByIdAndIsDeletedFalse(id)
   → SELECT * FROM customers WHERE id = ? AND is_deleted = false
   → Customer entity loaded into persistence context

7. Service checks optimistic locking
   → if (customer.version != request.version) throw OptimisticLockException
   → Version matches (both = 5), continue

8. Service captures old values for cascading
   → String oldCompanyName = "Acme Corp"

9. Service calls customer.beforeUpdate(request, false, customerRepository)
   → Validates companyName not blank ✓
   → Checks email uniqueness (if changed)
   → Validates no read-only fields in user request ✓
   → No exceptions thrown

10. Service calls customer.update(request, false)
    → customer.companyName = "Acme Corporation"
    → customer.email = "billing@acme.com"
    → Other fields unchanged
    → @PreUpdate triggers in BaseEntity
      → customer.lastModifiedAt = now()
      → customer.lastModifiedBy = UserContext.getCurrentUser()

11. Service calls customerRepository.save(customer)
    → Hibernate queues UPDATE statement (not executed yet!)
    → UPDATE customers SET
        company_name = 'Acme Corporation',
        email = 'billing@acme.com',
        last_modified_at = '2025-01-15 10:30:00',
        last_modified_by = 'user-uuid',
        version = 6
      WHERE id = '123e4567...' AND version = 5

12. Service calls customer.afterUpdate(oldCompanyName, customerRepository, invoiceService)
    → Detects companyName changed: "Acme Corp" → "Acme Corporation"
    → Loads related invoices:
      → customerRepository.getInvoicesForCustomer(customerId)
      → SELECT * FROM invoices WHERE customer_id = ? AND is_deleted = false
      → Returns [Invoice#1, Invoice#2] from persistence context

    → For Invoice #1:
      → Creates UpdateInvoiceRequest
        → id = invoice1.id
        → version = invoice1.version
        → customerName = "Acme Corporation"

      → Calls invoiceService.systemUpdateInvoice(request)
        → @Transactional(MANDATORY) joins parent transaction ✓
        → Loads Invoice#1 (already in persistence context)
        → invoice1.beforeUpdate(request, true)  // isSystemUpdate=true
          → Skips "status must be DRAFT" check for system updates ✓
        → invoice1.update(request, true)
          → invoice1.customerName = "Acme Corporation"
        → invoiceRepository.save(invoice1)
          → UPDATE queued (not executed yet!)
        → invoice1.afterUpdate(oldCustomerName, invoiceRepo, lineItemService, paymentService)
          → Detects customerName changed
          → Loads line items for Invoice#1
            → invoiceRepository.getLineItemsForInvoice(invoice1.id)
            → Returns [LineItem#1, LineItem#2, LineItem#3]

          → For each LineItem (3 total):
            → Creates UpdateLineItemRequest with new customerName
            → Calls lineItemService.systemUpdateLineItem(request)
              → @Transactional(MANDATORY) joins same transaction ✓
              → lineItem.update(request, true)
              → lineItemRepository.save(lineItem)
              → UPDATE queued for each line item

          → Loads payments for Invoice#1 (none exist)

    → For Invoice #2:
      → Same process as Invoice#1
      → Updates Invoice#2 (queued)
      → Cascades to 2 line items (queued)
      → Cascades to 1 payment (queued)

13. Service returns customerMapper.toResponse(customer)
    → Converts Customer entity to CustomerResponse DTO
    → Includes all fields including updated companyName

14. @Transactional boundary ends
    ↓
    → All queued updates execute NOW in single batch:
      → UPDATE customers... (1 row)
      → UPDATE invoices... (2 rows)
      → UPDATE line_items... (5 rows)
      → UPDATE payments... (1 row)
    → Database transaction COMMITS
    → All changes now visible to other transactions

15. Controller receives CustomerResponse
    → Wraps in ApiResponse.success()
    → Returns ResponseEntity with HTTP 200

16. Finally block executes
    → UserContext.clear()
    → Removes userId from ThreadLocal

17. HTTP Response sent to client
```

**Final Database State:**
```
customers:
  - id: 123e4567...
    company_name: "Acme Corporation"  ← CHANGED
    email: "billing@acme.com"
    version: 6  ← INCREMENTED
    last_modified_at: 2025-01-15 10:30:00
    last_modified_by: <original-user-id>

invoices (2 rows updated):
  - invoice #1: customer_name = "Acme Corporation", version++
  - invoice #2: customer_name = "Acme Corporation", version++

line_items (5 rows updated):
  - All 5 line items: customer_name = "Acme Corporation", version++

payments (1 row updated):
  - payment #1: customer_name = "Acme Corporation", version++
```

**If Any Exception Occurred:**
- Entire transaction rolls back
- NO database changes
- Customer still has old values
- All related entities unchanged
- Exception propagates to GlobalExceptionHandler
- Returns appropriate error response to client

---

## Validation Rules

### Field Validation (Bean Validation)
- `companyName`: @NotBlank
- `email`: @Email (valid format), unique across non-deleted customers

### Business Validation (beforeX methods)

**beforeCreate:**
- Company name required and not blank
- Email must be unique (if provided)

**beforeUpdate:**
- Company name required and not blank
- Email must be unique (if changing email)
- User cannot update read-only fields (draftInvoiceCount, sentInvoiceCount, paidInvoiceCount, totalOutstanding)
- System CAN update read-only fields

**beforeDelete:**
- Customer cannot have any invoices with status = SENT
- Can delete if only DRAFT or PAID invoices exist (they will cascade delete)

---

## Testing Strategy

### Unit Tests
- Test entity lifecycle methods in isolation
- Mock repositories and services
- Verify validation logic

### Integration Tests
```java
@SpringBootTest
@Transactional
class CustomerIntegrationTest {

    @Test
    void testCreateCustomer() {
        // Create customer via API
        // Verify in database
        // Check audit fields populated
    }

    @Test
    void testUpdateCustomerCascadesToInvoices() {
        // Create customer with invoices
        // Update customer name
        // Verify all related entities updated
        // Verify all in same transaction
    }

    @Test
    void testCannotDeleteCustomerWithSentInvoices() {
        // Create customer with SENT invoice
        // Attempt delete
        // Verify ValidationException thrown
        // Verify customer still exists
    }

    @Test
    void testOptimisticLocking() {
        // Load customer (version 1)
        // Simulate concurrent update (version → 2)
        // Attempt update with version 1
        // Verify OptimisticLockException thrown
    }
}
```

---

## Mock Interactions with Other Services

### InvoiceService (Mocked for Now)
```java
package com.invoiceme.application.invoice;

import com.invoiceme.application.invoice.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InvoiceService {

    @Transactional(propagation = Propagation.MANDATORY)
    public InvoiceResponse systemUpdateInvoice(UpdateInvoiceRequest request) {
        // TODO: Implement when building Invoice entity
        // For now, just a placeholder
        return new InvoiceResponse();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void systemDeleteInvoice(UUID invoiceId) {
        // TODO: Implement when building Invoice entity
    }
}
```

### UpdateInvoiceRequest (Mock DTO)
```java
package com.invoiceme.application.invoice.dto;

import java.util.UUID;

public class UpdateInvoiceRequest {
    private UUID id;
    private Long version;
    private String customerName;

    // Getters and setters
}
```

### InvoiceResponse (Mock DTO)
```java
package com.invoiceme.application.invoice.dto;

public class InvoiceResponse {
    // TODO: Implement when building Invoice entity
}
```
