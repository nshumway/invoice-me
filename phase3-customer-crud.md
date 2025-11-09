# Phase 3: Customer CRUD (Vertical Slices)

## Overview
This phase implements complete Customer CRUD functionality using vertical slices. Each sub-phase builds one complete feature from backend through frontend with tests at every layer. We follow DDD, CQRS, and Vertical Slice Architecture principles.

**Goal:** Users can create, list, view, update, and delete customers through a polished UI.

**Duration Estimate:** 3-4 days

---

## Architecture Reference

**Domain-Driven Design (DDD):**
- Customer entity contains business logic in beforeX/X/afterX methods
- Rich domain model, not anemic entities
- Domain exceptions for business rule violations

**CQRS:**
- Commands (Create, Update, Delete): Different DTOs, validation, transactional
- Queries (Get, List): Optimized DTOs, read-only, lightweight

**Vertical Slice Architecture:**
- Each feature (Create, List, View, Update, Delete) built end-to-end
- Domain → Application → Infrastructure → UI
- Each slice independently testable

**Key Patterns:**
- Optimistic locking with version field
- Soft deletes with isDeleted flag
- Audit trail (createdBy, lastModifiedBy via UserContext)
- User vs System updates (isSystemUpdate flag)

---

# Phase 3A: Create Customer (Full Stack)

## US-19: Create Customer - Database & Entity

**As a developer, I need Customer entity so that customer data can be modeled**

**Acceptance Criteria:**
- Customer entity extends BaseEntity
- All fields defined per data.md
- beforeCreate, create, afterCreate methods implemented
- Database migration creates customers table
- Unit tests verify entity behavior

**Implementation Details:**

### 1. Create Database Migration

Create `src/main/resources/db/migration/V2__create_customers_table.sql`:

```sql
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    contact_first_name VARCHAR(255),
    contact_last_name VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    zip_code VARCHAR(20),
    country VARCHAR(100),

    -- Read-only computed fields (updated by system)
    draft_invoice_count INTEGER NOT NULL DEFAULT 0,
    sent_invoice_count INTEGER NOT NULL DEFAULT 0,
    paid_invoice_count INTEGER NOT NULL DEFAULT 0,
    total_outstanding DECIMAL(19, 2) NOT NULL DEFAULT 0.00,

    -- Audit fields
    created_at TIMESTAMP NOT NULL,
    created_by UUID NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    last_modified_by UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    -- Soft delete
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID
);

-- Unique email for non-deleted customers
CREATE UNIQUE INDEX idx_customers_email_active
    ON customers(email)
    WHERE is_deleted = FALSE;

-- Index for customer lookups
CREATE INDEX idx_customers_company_name
    ON customers(company_name)
    WHERE is_deleted = FALSE;
```

### 2. Create Customer Entity

Create `src/main/java/com/invoiceme/domain/customer/Customer.java`:

```java
package com.invoiceme.domain.customer;

import com.invoiceme.domain.common.BaseEntity;
import com.invoiceme.domain.common.exceptions.ValidationException;
import com.invoiceme.application.customer.dto.CreateCustomerRequest;
import com.invoiceme.application.customer.dto.UpdateCustomerRequest;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
import jakarta.persistence.*;
import java.math.BigDecimal;

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
    // (Will implement in Phase 3D)

    // === DELETE OPERATION ===
    // (Will implement in Phase 3E)

    // === Getters and Setters ===

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactFirstName() {
        return contactFirstName;
    }

    public void setContactFirstName(String contactFirstName) {
        this.contactFirstName = contactFirstName;
    }

    public String getContactLastName() {
        return contactLastName;
    }

    public void setContactLastName(String contactLastName) {
        this.contactLastName = contactLastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getDraftInvoiceCount() {
        return draftInvoiceCount;
    }

    public Integer getSentInvoiceCount() {
        return sentInvoiceCount;
    }

    public Integer getPaidInvoiceCount() {
        return paidInvoiceCount;
    }

    public BigDecimal getTotalOutstanding() {
        return totalOutstanding;
    }
}
```

**Testing:**

Create `src/test/java/com/invoiceme/domain/customer/CustomerTest.java`:

```java
package com.invoiceme.domain.customer;

import com.invoiceme.application.customer.dto.CreateCustomerRequest;
import com.invoiceme.domain.common.exceptions.ValidationException;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerTest {

    @Mock
    private CustomerRepository customerRepository;

    @Test
    void testCreateCustomerSuccess() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("Acme Corp");
        request.setEmail("contact@acme.com");
        request.setContactFirstName("John");
        request.setContactLastName("Doe");

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
        assertEquals(0, customer.getDraftInvoiceCount());
        assertEquals(0, customer.getSentInvoiceCount());
        assertEquals(0, customer.getPaidInvoiceCount());
        assertEquals(BigDecimal.ZERO, customer.getTotalOutstanding());
    }

    @Test
    void testBeforeCreateThrowsExceptionWhenCompanyNameBlank() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("");

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
        // All other fields null

        when(customerRepository.existsByEmailAndIsDeletedFalse(null))
                .thenReturn(false);

        Customer customer = new Customer();
        customer.beforeCreate(request, customerRepository);
        customer.create(request);

        assertEquals("Minimal Corp", customer.getCompanyName());
        assertNull(customer.getEmail());
        assertNull(customer.getContactFirstName());
        assertNull(customer.getPhone());
    }
}
```

---

## US-20: Create Customer - Repository

**As a developer, I need CustomerRepository so that customers can be persisted**

**Acceptance Criteria:**
- CustomerRepository extends JpaRepository
- save() and findById() methods available
- existsByEmailAndIsDeletedFalse() method for validation
- Integration test verifies persistence

**Implementation Details:**

Create `src/main/java/com/invoiceme/infrastructure/persistence/CustomerRepository.java`:

```java
package com.invoiceme.infrastructure.persistence;

import com.invoiceme.domain.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByIdAndIsDeletedFalse(UUID id);

    boolean existsByEmailAndIsDeletedFalse(String email);
}
```

**Testing:**

Create `src/test/java/com/invoiceme/infrastructure/persistence/CustomerRepositoryTest.java`:

```java
package com.invoiceme.infrastructure.persistence;

import com.invoiceme.application.common.UserContext;
import com.invoiceme.application.customer.dto.CreateCustomerRequest;
import com.invoiceme.domain.customer.Customer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
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

    @Test
    void testSaveAndFindById() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("Test Company");
        request.setEmail("test@example.com");

        Customer customer = new Customer();
        customer.beforeCreate(request, customerRepository);
        customer.create(request);

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
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("Email Test Corp");
        request.setEmail("unique@example.com");

        Customer customer = new Customer();
        customer.beforeCreate(request, customerRepository);
        customer.create(request);
        customerRepository.save(customer);

        assertTrue(customerRepository.existsByEmailAndIsDeletedFalse("unique@example.com"));
    }

    @Test
    void testExistsByEmailReturnsFalseWhenNotExists() {
        assertFalse(customerRepository.existsByEmailAndIsDeletedFalse("nonexistent@example.com"));
    }

    @Test
    void testFindByIdExcludesDeletedCustomers() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("To Be Deleted");

        Customer customer = new Customer();
        customer.beforeCreate(request, customerRepository);
        customer.create(request);
        Customer saved = customerRepository.save(customer);

        UUID customerId = saved.getId();

        // Soft delete
        saved.markAsDeleted();
        customerRepository.save(saved);

        // Should not find deleted customer
        Optional<Customer> found = customerRepository.findByIdAndIsDeletedFalse(customerId);
        assertFalse(found.isPresent());
    }
}
```

---

## US-21: Create Customer - DTOs & Mapper

**As a developer, I need DTOs and mapper so that API boundaries are clean**

**Acceptance Criteria:**
- CreateCustomerRequest with validation annotations
- CustomerResponse with all fields
- CustomerMapper converts entity to DTO
- Unit tests verify mapping

**Implementation Details:**

### 1. Create DTOs

Create `src/main/java/com/invoiceme/application/customer/dto/CreateCustomerRequest.java`:

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

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactFirstName() {
        return contactFirstName;
    }

    public void setContactFirstName(String contactFirstName) {
        this.contactFirstName = contactFirstName;
    }

    public String getContactLastName() {
        return contactLastName;
    }

    public void setContactLastName(String contactLastName) {
        this.contactLastName = contactLastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
```

Create `src/main/java/com/invoiceme/application/customer/dto/CustomerResponse.java`:

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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(Instant lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public UUID getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(UUID lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactFirstName() {
        return contactFirstName;
    }

    public void setContactFirstName(String contactFirstName) {
        this.contactFirstName = contactFirstName;
    }

    public String getContactLastName() {
        return contactLastName;
    }

    public void setContactLastName(String contactLastName) {
        this.contactLastName = contactLastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getDraftInvoiceCount() {
        return draftInvoiceCount;
    }

    public void setDraftInvoiceCount(Integer draftInvoiceCount) {
        this.draftInvoiceCount = draftInvoiceCount;
    }

    public Integer getSentInvoiceCount() {
        return sentInvoiceCount;
    }

    public void setSentInvoiceCount(Integer sentInvoiceCount) {
        this.sentInvoiceCount = sentInvoiceCount;
    }

    public Integer getPaidInvoiceCount() {
        return paidInvoiceCount;
    }

    public void setPaidInvoiceCount(Integer paidInvoiceCount) {
        this.paidInvoiceCount = paidInvoiceCount;
    }

    public BigDecimal getTotalOutstanding() {
        return totalOutstanding;
    }

    public void setTotalOutstanding(BigDecimal totalOutstanding) {
        this.totalOutstanding = totalOutstanding;
    }
}
```

### 2. Create Mapper

Create `src/main/java/com/invoiceme/application/customer/CustomerMapper.java`:

```java
package com.invoiceme.application.customer;

import com.invoiceme.application.customer.dto.CustomerResponse;
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
}
```

**Testing:**

Create `src/test/java/com/invoiceme/application/customer/CustomerMapperTest.java`:

```java
package com.invoiceme.application.customer;

import com.invoiceme.application.common.UserContext;
import com.invoiceme.application.customer.dto.CreateCustomerRequest;
import com.invoiceme.application.customer.dto.CustomerResponse;
import com.invoiceme.domain.customer.Customer;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerMapperTest {

    private CustomerMapper mapper;

    @Mock
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        mapper = new CustomerMapper();
        UserContext.setCurrentUser(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void testToResponse() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("Test Corp");
        request.setEmail("test@example.com");
        request.setContactFirstName("John");
        request.setContactLastName("Doe");
        request.setPhone("555-1234");
        request.setCity("New York");

        when(customerRepository.existsByEmailAndIsDeletedFalse(anyString())).thenReturn(false);

        Customer customer = new Customer();
        customer.beforeCreate(request, customerRepository);
        customer.create(request);

        // Simulate what JPA would set
        customer.setId(UUID.randomUUID());

        CustomerResponse response = mapper.toResponse(customer);

        assertNotNull(response.getId());
        assertEquals("Test Corp", response.getCompanyName());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("John", response.getContactFirstName());
        assertEquals("Doe", response.getContactLastName());
        assertEquals("555-1234", response.getPhone());
        assertEquals("New York", response.getCity());
        assertEquals(0, response.getDraftInvoiceCount());
        assertEquals(0, response.getSentInvoiceCount());
        assertEquals(0, response.getPaidInvoiceCount());
        assertEquals(BigDecimal.ZERO, response.getTotalOutstanding());
    }
}
```

---

## US-22: Create Customer - Service

**As a developer, I need CustomerService.createCustomer() so that business logic is orchestrated**

**Acceptance Criteria:**
- CustomerService.createCustomer() method
- Follows beforeCreate → create → save → afterCreate flow
- Unit tests with mocked repository
- Integration test for full create flow

**Implementation Details:**

Create `src/main/java/com/invoiceme/application/customer/CustomerService.java`:

```java
package com.invoiceme.application.customer;

import com.invoiceme.application.customer.dto.CreateCustomerRequest;
import com.invoiceme.application.customer.dto.CustomerResponse;
import com.invoiceme.domain.customer.Customer;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerMapper customerMapper;

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
}
```

**Testing:**

Create `src/test/java/com/invoiceme/application/customer/CustomerServiceTest.java`:

```java
package com.invoiceme.application.customer;

import com.invoiceme.application.common.UserContext;
import com.invoiceme.application.customer.dto.CreateCustomerRequest;
import com.invoiceme.application.customer.dto.CustomerResponse;
import com.invoiceme.domain.common.exceptions.ValidationException;
import com.invoiceme.domain.customer.Customer;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        UserContext.setCurrentUser(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void testCreateCustomerSuccess() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("Test Corp");
        request.setEmail("test@example.com");

        when(customerRepository.existsByEmailAndIsDeletedFalse("test@example.com"))
                .thenReturn(false);

        Customer savedCustomer = new Customer();
        savedCustomer.setId(UUID.randomUUID());
        when(customerRepository.save(any(Customer.class))).thenReturn(savedCustomer);

        CustomerResponse expectedResponse = new CustomerResponse();
        expectedResponse.setId(savedCustomer.getId());
        expectedResponse.setCompanyName("Test Corp");
        when(customerMapper.toResponse(any(Customer.class))).thenReturn(expectedResponse);

        CustomerResponse response = customerService.createCustomer(request);

        assertNotNull(response);
        assertEquals("Test Corp", response.getCompanyName());
        verify(customerRepository, times(1)).save(any(Customer.class));
        verify(customerMapper, times(1)).toResponse(any(Customer.class));
    }

    @Test
    void testCreateCustomerThrowsExceptionWhenEmailExists() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("Test Corp");
        request.setEmail("existing@example.com");

        when(customerRepository.existsByEmailAndIsDeletedFalse("existing@example.com"))
                .thenReturn(true);

        assertThrows(ValidationException.class, () ->
            customerService.createCustomer(request));

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void testCreateCustomerThrowsExceptionWhenCompanyNameBlank() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("");

        assertThrows(ValidationException.class, () ->
            customerService.createCustomer(request));

        verify(customerRepository, never()).save(any(Customer.class));
    }
}
```

Create integration test `src/test/java/com/invoiceme/application/customer/CustomerServiceIntegrationTest.java`:

```java
package com.invoiceme.application.customer;

import com.invoiceme.application.common.UserContext;
import com.invoiceme.application.customer.dto.CreateCustomerRequest;
import com.invoiceme.application.customer.dto.CustomerResponse;
import com.invoiceme.domain.common.exceptions.ValidationException;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CustomerServiceIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        UserContext.setCurrentUser(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void testCreateCustomerFullFlow() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("Integration Test Corp");
        request.setEmail("integration@example.com");
        request.setContactFirstName("Jane");
        request.setContactLastName("Smith");
        request.setPhone("555-9999");
        request.setAddressLine1("123 Main St");
        request.setCity("Boston");
        request.setState("MA");
        request.setZipCode("02101");
        request.setCountry("USA");

        CustomerResponse response = customerService.createCustomer(request);

        assertNotNull(response.getId());
        assertEquals("Integration Test Corp", response.getCompanyName());
        assertEquals("integration@example.com", response.getEmail());
        assertEquals("Jane", response.getContactFirstName());
        assertEquals("Smith", response.getContactLastName());
        assertEquals("555-9999", response.getPhone());
        assertEquals("123 Main St", response.getAddressLine1());
        assertEquals("Boston", response.getCity());
        assertEquals("MA", response.getState());
        assertEquals("02101", response.getZipCode());
        assertEquals("USA", response.getCountry());

        // Verify audit fields
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getCreatedBy());
        assertNotNull(response.getLastModifiedAt());
        assertNotNull(response.getLastModifiedBy());
        assertEquals(0L, response.getVersion());

        // Verify read-only fields initialized
        assertEquals(0, response.getDraftInvoiceCount());
        assertEquals(0, response.getSentInvoiceCount());
        assertEquals(0, response.getPaidInvoiceCount());
        assertEquals(BigDecimal.ZERO, response.getTotalOutstanding());

        // Verify saved in database
        assertTrue(customerRepository.findByIdAndIsDeletedFalse(response.getId()).isPresent());
    }

    @Test
    void testCreateCustomerWithDuplicateEmailFails() {
        // Create first customer
        CreateCustomerRequest request1 = new CreateCustomerRequest();
        request1.setCompanyName("First Corp");
        request1.setEmail("duplicate@example.com");
        customerService.createCustomer(request1);

        // Try to create second customer with same email
        CreateCustomerRequest request2 = new CreateCustomerRequest();
        request2.setCompanyName("Second Corp");
        request2.setEmail("duplicate@example.com");

        ValidationException exception = assertThrows(ValidationException.class, () ->
            customerService.createCustomer(request2));

        assertTrue(exception.getMessage().contains("already exists"));
    }
}
```

---

## US-23: Create Customer - Controller

**As a developer, I need POST /api/customers endpoint so that customers can be created via REST API**

**Acceptance Criteria:**
- POST /api/customers endpoint
- Returns 201 Created on success
- Returns ApiResponse wrapper
- Integration test with MockMvc

**Implementation Details:**

Create `src/main/java/com/invoiceme/infrastructure/web/CustomerController.java`:

```java
package com.invoiceme.infrastructure.web;

import com.invoiceme.application.common.ApiResponse;
import com.invoiceme.application.customer.CustomerService;
import com.invoiceme.application.customer.dto.CreateCustomerRequest;
import com.invoiceme.application.customer.dto.CustomerResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
```

**Testing:**

Create `src/test/java/com/invoiceme/infrastructure/web/CustomerControllerTest.java`:

```java
package com.invoiceme.infrastructure.web;

import com.invoiceme.application.common.UserContext;
import com.invoiceme.infrastructure.persistence.UserRepository;
import com.invoiceme.infrastructure.security.JwtTokenProvider;
import com.invoiceme.domain.user.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String authToken;

    @BeforeEach
    void setUp() {
        // Create test user
        User testUser = new User();
        UUID userId = UUID.randomUUID();
        UserContext.setCurrentUser(userId);
        testUser.create("test@example.com", "hashedPassword", "Test", "User");
        testUser.setId(userId);
        userRepository.save(testUser);
        UserContext.clear();

        // Generate JWT token
        authToken = jwtTokenProvider.generateToken(testUser.getId(), testUser.getEmail());
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void testCreateCustomerSuccess() throws Exception {
        String requestBody = """
            {
                "companyName": "API Test Corp",
                "email": "api@test.com",
                "contactFirstName": "John",
                "contactLastName": "Doe",
                "phone": "555-1234",
                "addressLine1": "123 Test St",
                "city": "Boston",
                "state": "MA",
                "zipCode": "02101",
                "country": "USA"
            }
            """;

        mockMvc.perform(post("/api/customers")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Customer created successfully"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.companyName").value("API Test Corp"))
                .andExpect(jsonPath("$.data.email").value("api@test.com"))
                .andExpect(jsonPath("$.data.contactFirstName").value("John"))
                .andExpect(jsonPath("$.data.contactLastName").value("Doe"))
                .andExpect(jsonPath("$.data.phone").value("555-1234"))
                .andExpect(jsonPath("$.data.addressLine1").value("123 Test St"))
                .andExpect(jsonPath("$.data.city").value("Boston"))
                .andExpect(jsonPath("$.data.state").value("MA"))
                .andExpect(jsonPath("$.data.zipCode").value("02101"))
                .andExpect(jsonPath("$.data.country").value("USA"))
                .andExpect(jsonPath("$.data.version").value(0))
                .andExpect(jsonPath("$.data.draftInvoiceCount").value(0))
                .andExpect(jsonPath("$.data.sentInvoiceCount").value(0))
                .andExpect(jsonPath("$.data.paidInvoiceCount").value(0))
                .andExpect(jsonPath("$.data.totalOutstanding").value(0))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.createdBy").exists());
    }

    @Test
    void testCreateCustomerWithMinimalFields() throws Exception {
        String requestBody = """
            {
                "companyName": "Minimal Corp"
            }
            """;

        mockMvc.perform(post("/api/customers")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.companyName").value("Minimal Corp"))
                .andExpect(jsonPath("$.data.email").isEmpty());
    }

    @Test
    void testCreateCustomerValidationErrorBlankCompanyName() throws Exception {
        String requestBody = """
            {
                "companyName": "",
                "email": "test@example.com"
            }
            """;

        mockMvc.perform(post("/api/customers")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testCreateCustomerValidationErrorInvalidEmail() throws Exception {
        String requestBody = """
            {
                "companyName": "Test Corp",
                "email": "invalid-email"
            }
            """;

        mockMvc.perform(post("/api/customers")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("email")));
    }

    @Test
    void testCreateCustomerUnauthorized() throws Exception {
        String requestBody = """
            {
                "companyName": "Test Corp"
            }
            """;

        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isUnauthorized());
    }
}
```

---

## US-24: Create Customer - Frontend Models & API

**As a frontend developer, I need TypeScript models and API client so that I can call the create endpoint**

**Acceptance Criteria:**
- Customer TypeScript interfaces defined
- customerApi.create() method implemented
- Matches backend DTOs exactly

**Implementation Details:**

### 1. Create Models

Create `src/models/Customer.ts`:

```typescript
export interface Customer {
  // Entity ID and audit fields
  id: string;
  createdAt: string;
  createdBy: string;
  lastModifiedAt: string;
  lastModifiedBy: string;
  version: number;

  // Customer fields
  companyName: string;
  contactFirstName: string | null;
  contactLastName: string | null;
  email: string | null;
  phone: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  state: string | null;
  zipCode: string | null;
  country: string | null;

  // Read-only computed fields
  draftInvoiceCount: number;
  sentInvoiceCount: number;
  paidInvoiceCount: number;
  totalOutstanding: string; // BigDecimal as string
}

export interface CreateCustomerRequest {
  companyName: string;
  contactFirstName?: string;
  contactLastName?: string;
  email?: string;
  phone?: string;
  addressLine1?: string;
  addressLine2?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  country?: string;
}
```

### 2. Create API Client

Create `src/api/customerApi.ts`:

```typescript
import { apiClient } from './client';
import { ApiResponse } from '../models/ApiResponse';
import { Customer, CreateCustomerRequest } from '../models/Customer';

export const customerApi = {
  // POST /api/customers
  create: async (request: CreateCustomerRequest): Promise<Customer> => {
    const response = await apiClient.post<ApiResponse<Customer>>('/customers', request);
    return response.data.data;
  },
};
```

**Testing:**
- Manual test will be done in US-25 with full form

---

## US-25: Create Customer - Form View & ViewModel

**As a user, I need a form to create customers so that I can add new customers to the system**

**Acceptance Criteria:**
- Form with all customer fields
- Company name is required
- Email validation
- Inline validation errors
- Submits to API and shows loading state
- Navigates to customer list on success
- Shows error messages on failure

**Implementation Details:**

### 1. Create ViewModel

Create `src/viewmodels/customers/CustomerFormViewModel.ts`:

```typescript
import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { customerApi } from '../../api/customerApi';
import { CreateCustomerRequest } from '../../models/Customer';

export const CustomerFormViewModel = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // Form fields
  const [companyName, setCompanyName] = useState('');
  const [email, setEmail] = useState('');
  const [contactFirstName, setContactFirstName] = useState('');
  const [contactLastName, setContactLastName] = useState('');
  const [phone, setPhone] = useState('');
  const [addressLine1, setAddressLine1] = useState('');
  const [addressLine2, setAddressLine2] = useState('');
  const [city, setCity] = useState('');
  const [state, setState] = useState('');
  const [zipCode, setZipCode] = useState('');
  const [country, setCountry] = useState('');

  // Validation errors
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Create mutation
  const createMutation = useMutation({
    mutationFn: customerApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers', 'list'] });
      navigate('/customers');
    },
    onError: (error: any) => {
      if (error.response?.data?.message) {
        setErrors({ submit: error.response.data.message });
      } else {
        setErrors({ submit: 'Failed to create customer' });
      }
    },
  });

  // Validation
  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!companyName.trim()) {
      newErrors.companyName = 'Company name is required';
    }

    if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      newErrors.email = 'Must be a valid email address';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // Submit handler
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (!validate()) return;

    const formData: CreateCustomerRequest = {
      companyName,
      email: email || undefined,
      contactFirstName: contactFirstName || undefined,
      contactLastName: contactLastName || undefined,
      phone: phone || undefined,
      addressLine1: addressLine1 || undefined,
      addressLine2: addressLine2 || undefined,
      city: city || undefined,
      state: state || undefined,
      zipCode: zipCode || undefined,
      country: country || undefined,
    };

    createMutation.mutate(formData);
  };

  const handleCancel = () => {
    navigate('/customers');
  };

  return {
    // Form state
    companyName,
    setCompanyName,
    email,
    setEmail,
    contactFirstName,
    setContactFirstName,
    contactLastName,
    setContactLastName,
    phone,
    setPhone,
    addressLine1,
    setAddressLine1,
    addressLine2,
    setAddressLine2,
    city,
    setCity,
    state,
    setState,
    zipCode,
    setZipCode,
    country,
    setCountry,

    // Validation
    errors,

    // Actions
    handleSubmit,
    handleCancel,

    // Loading state
    isSubmitting: createMutation.isPending,
  };
};
```

### 2. Create View

Create `src/views/customers/CustomerFormView.tsx`:

```typescript
import React from 'react';
import { CustomerFormViewModel } from '../../viewmodels/customers/CustomerFormViewModel';

export const CustomerFormView: React.FC = () => {
  const vm = CustomerFormViewModel();

  return (
    <div className="max-w-2xl mx-auto p-6">
      <h1 className="text-3xl font-bold mb-6">Create Customer</h1>

      <form onSubmit={vm.handleSubmit} className="space-y-6">
        {/* Company Name */}
        <div>
          <label className="block text-sm font-medium mb-2">
            Company Name <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={vm.companyName}
            onChange={(e) => vm.setCompanyName(e.target.value)}
            className={`w-full border rounded px-3 py-2 ${
              vm.errors.companyName ? 'border-red-500' : 'border-gray-300'
            }`}
          />
          {vm.errors.companyName && (
            <p className="text-red-500 text-sm mt-1">{vm.errors.companyName}</p>
          )}
        </div>

        {/* Email */}
        <div>
          <label className="block text-sm font-medium mb-2">Email</label>
          <input
            type="email"
            value={vm.email}
            onChange={(e) => vm.setEmail(e.target.value)}
            className={`w-full border rounded px-3 py-2 ${
              vm.errors.email ? 'border-red-500' : 'border-gray-300'
            }`}
          />
          {vm.errors.email && (
            <p className="text-red-500 text-sm mt-1">{vm.errors.email}</p>
          )}
        </div>

        {/* Contact Name */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium mb-2">Contact First Name</label>
            <input
              type="text"
              value={vm.contactFirstName}
              onChange={(e) => vm.setContactFirstName(e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-2">Contact Last Name</label>
            <input
              type="text"
              value={vm.contactLastName}
              onChange={(e) => vm.setContactLastName(e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2"
            />
          </div>
        </div>

        {/* Phone */}
        <div>
          <label className="block text-sm font-medium mb-2">Phone</label>
          <input
            type="tel"
            value={vm.phone}
            onChange={(e) => vm.setPhone(e.target.value)}
            className="w-full border border-gray-300 rounded px-3 py-2"
          />
        </div>

        {/* Address */}
        <div>
          <label className="block text-sm font-medium mb-2">Address Line 1</label>
          <input
            type="text"
            value={vm.addressLine1}
            onChange={(e) => vm.setAddressLine1(e.target.value)}
            className="w-full border border-gray-300 rounded px-3 py-2"
          />
        </div>

        <div>
          <label className="block text-sm font-medium mb-2">Address Line 2</label>
          <input
            type="text"
            value={vm.addressLine2}
            onChange={(e) => vm.setAddressLine2(e.target.value)}
            className="w-full border border-gray-300 rounded px-3 py-2"
          />
        </div>

        {/* City, State, Zip */}
        <div className="grid grid-cols-3 gap-4">
          <div className="col-span-2">
            <label className="block text-sm font-medium mb-2">City</label>
            <input
              type="text"
              value={vm.city}
              onChange={(e) => vm.setCity(e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-2">State</label>
            <input
              type="text"
              value={vm.state}
              onChange={(e) => vm.setState(e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2"
            />
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium mb-2">Zip Code</label>
            <input
              type="text"
              value={vm.zipCode}
              onChange={(e) => vm.setZipCode(e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2"
            />
          </div>
          <div>
            <label className="block text-sm font-medium mb-2">Country</label>
            <input
              type="text"
              value={vm.country}
              onChange={(e) => vm.setCountry(e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2"
            />
          </div>
        </div>

        {/* Submit Error */}
        {vm.errors.submit && (
          <div className="bg-red-50 border border-red-300 rounded p-3">
            <p className="text-red-700 text-sm">{vm.errors.submit}</p>
          </div>
        )}

        {/* Actions */}
        <div className="flex gap-3">
          <button
            type="submit"
            disabled={vm.isSubmitting}
            className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700 disabled:opacity-50"
          >
            {vm.isSubmitting ? 'Creating...' : 'Create Customer'}
          </button>
          <button
            type="button"
            onClick={vm.handleCancel}
            className="bg-gray-300 text-gray-700 px-6 py-2 rounded hover:bg-gray-400"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
};
```

### 3. Add Route

Update `src/App.tsx` to add the route:

```typescript
// Add import
import { CustomerFormView } from './views/customers/CustomerFormView';

// Add route inside ProtectedRoute
<Route path="/customers/new" element={<CustomerFormView />} />
```

**Manual Testing:**
1. Start backend: `mvn spring-boot:run`
2. Start frontend: `npm run dev`
3. Login with test account
4. Navigate to http://localhost:5173/customers/new
5. Fill out form with:
   - Company Name: "Test Company"
   - Email: "test@company.com"
   - First Name: "John"
   - Last Name: "Doe"
6. Click "Create Customer"
7. Verify redirected to /customers
8. Check database: `SELECT * FROM customers;`
9. Verify customer created

**Error Testing:**
1. Try submitting with blank company name → validation error
2. Try submitting with invalid email → validation error
3. Create customer with email "test@company.com"
4. Try creating another with same email → backend error displayed

---

## US-26: Create Customer - Integration Test

**As a developer, I need end-to-end integration test so that I can verify the full create flow works**

**Acceptance Criteria:**
- Test creates user, logs in, creates customer
- Verifies customer in database
- Tests error cases

**Testing:**

This can be added to the existing `CustomerControllerTest.java`. The tests in US-23 already cover end-to-end integration for the create flow.

**Additional E2E Test (Optional):**

If you want a separate E2E test class:

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void testFullCustomerCreationFlow() throws Exception {
        // 1. Create user (signup)
        String signupBody = """
            {
                "email": "e2e@example.com",
                "password": "password123",
                "firstName": "E2E",
                "lastName": "Test"
            }
            """;

        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(signupBody))
                .andExpect(status().isCreated())
                .andReturn();

        String signupResponse = signupResult.getResponse().getContentAsString();
        String token = JsonPath.read(signupResponse, "$.data.token");

        // 2. Create customer
        String customerBody = """
            {
                "companyName": "E2E Test Corp",
                "email": "e2e@test.com",
                "contactFirstName": "Jane",
                "contactLastName": "Doe"
            }
            """;

        MvcResult customerResult = mockMvc.perform(post("/api/customers")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.companyName").value("E2E Test Corp"))
                .andReturn();

        String customerResponse = customerResult.getResponse().getContentAsString();
        String customerId = JsonPath.read(customerResponse, "$.data.id");

        // 3. Verify in database
        Optional<Customer> customer = customerRepository.findByIdAndIsDeletedFalse(UUID.fromString(customerId));
        assertTrue(customer.isPresent());
        assertEquals("E2E Test Corp", customer.get().getCompanyName());
        assertEquals("e2e@test.com", customer.get().getEmail());
    }
}
```

---

# Phase 3B: List Customers (Full Stack)

## US-27: List Customers - Repository Query

**As a developer, I need repository method to list all customers so that I can retrieve customers from database**

**Acceptance Criteria:**
- findAllByIsDeletedFalseOrderByCompanyName() method
- Returns customers sorted by company name
- Excludes soft-deleted customers
- Integration test verifies sorting and filtering

**Implementation Details:**

Update `src/main/java/com/invoiceme/infrastructure/persistence/CustomerRepository.java`:

```java
package com.invoiceme.infrastructure.persistence;

import com.invoiceme.domain.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByIdAndIsDeletedFalse(UUID id);

    List<Customer> findAllByIsDeletedFalseOrderByCompanyName();

    boolean existsByEmailAndIsDeletedFalse(String email);
}
```

**Testing:**

Add to `CustomerRepositoryTest.java`:

```java
@Test
void testFindAllByIsDeletedFalseOrderByCompanyName() {
    // Create customers in random order
    createAndSaveCustomer("Zebra Corp", "zebra@example.com");
    createAndSaveCustomer("Apple Corp", "apple@example.com");
    createAndSaveCustomer("Microsoft Corp", "microsoft@example.com");

    List<Customer> customers = customerRepository.findAllByIsDeletedFalseOrderByCompanyName();

    assertEquals(3, customers.size());
    assertEquals("Apple Corp", customers.get(0).getCompanyName());
    assertEquals("Microsoft Corp", customers.get(1).getCompanyName());
    assertEquals("Zebra Corp", customers.get(2).getCompanyName());
}

@Test
void testFindAllExcludesDeletedCustomers() {
    Customer customer1 = createAndSaveCustomer("Active Corp", "active@example.com");
    Customer customer2 = createAndSaveCustomer("Deleted Corp", "deleted@example.com");

    // Soft delete customer2
    customer2.markAsDeleted();
    customerRepository.save(customer2);

    List<Customer> customers = customerRepository.findAllByIsDeletedFalseOrderByCompanyName();

    assertEquals(1, customers.size());
    assertEquals("Active Corp", customers.get(0).getCompanyName());
}

private Customer createAndSaveCustomer(String companyName, String email) {
    CreateCustomerRequest request = new CreateCustomerRequest();
    request.setCompanyName(companyName);
    request.setEmail(email);

    Customer customer = new Customer();
    customer.beforeCreate(request, customerRepository);
    customer.create(request);
    return customerRepository.save(customer);
}
```

---

## US-28: List Customers - DTO & Service

**As a developer, I need CustomerListItemResponse and service method so that I can return lightweight customer list**

**Acceptance Criteria:**
- CustomerListItemResponse with subset of fields
- CustomerService.listAllCustomers() method
- CustomerMapper.toListItem() method
- Unit and integration tests

**Implementation Details:**

### 1. Create DTO

Create `src/main/java/com/invoiceme/application/customer/dto/CustomerListItemResponse.java`:

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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BigDecimal getTotalOutstanding() {
        return totalOutstanding;
    }

    public void setTotalOutstanding(BigDecimal totalOutstanding) {
        this.totalOutstanding = totalOutstanding;
    }
}
```

### 2. Update Mapper

Add to `CustomerMapper.java`:

```java
public CustomerListItemResponse toListItem(Customer entity) {
    CustomerListItemResponse dto = new CustomerListItemResponse();

    dto.setId(entity.getId());
    dto.setCompanyName(entity.getCompanyName());
    dto.setEmail(entity.getEmail());
    dto.setTotalOutstanding(entity.getTotalOutstanding());

    return dto;
}
```

### 3. Update Service

Add to `CustomerService.java`:

```java
// === LIST ALL ===

@Transactional(readOnly = true)
public List<CustomerListItemResponse> listAllCustomers() {
    return customerRepository.findAllByIsDeletedFalseOrderByCompanyName()
            .stream()
            .map(customerMapper::toListItem)
            .collect(Collectors.toList());
}
```

Don't forget the import:
```java
import java.util.List;
import java.util.stream.Collectors;
import com.invoiceme.application.customer.dto.CustomerListItemResponse;
```

**Testing:**

Add to `CustomerMapperTest.java`:

```java
@Test
void testToListItem() {
    CreateCustomerRequest request = new CreateCustomerRequest();
    request.setCompanyName("List Test Corp");
    request.setEmail("list@test.com");

    when(customerRepository.existsByEmailAndIsDeletedFalse(anyString())).thenReturn(false);

    Customer customer = new Customer();
    customer.beforeCreate(request, customerRepository);
    customer.create(request);
    customer.setId(UUID.randomUUID());

    CustomerListItemResponse response = mapper.toListItem(customer);

    assertNotNull(response.getId());
    assertEquals("List Test Corp", response.getCompanyName());
    assertEquals("list@test.com", response.getEmail());
    assertEquals(BigDecimal.ZERO, response.getTotalOutstanding());
}
```

Add to `CustomerServiceIntegrationTest.java`:

```java
@Test
void testListAllCustomers() {
    // Create multiple customers
    CreateCustomerRequest request1 = new CreateCustomerRequest();
    request1.setCompanyName("Zebra Corp");
    request1.setEmail("zebra@example.com");
    customerService.createCustomer(request1);

    CreateCustomerRequest request2 = new CreateCustomerRequest();
    request2.setCompanyName("Apple Corp");
    request2.setEmail("apple@example.com");
    customerService.createCustomer(request2);

    CreateCustomerRequest request3 = new CreateCustomerRequest();
    request3.setCompanyName("Microsoft Corp");
    request3.setEmail("microsoft@example.com");
    customerService.createCustomer(request3);

    List<CustomerListItemResponse> customers = customerService.listAllCustomers();

    assertEquals(3, customers.size());
    assertEquals("Apple Corp", customers.get(0).getCompanyName());
    assertEquals("Microsoft Corp", customers.get(1).getCompanyName());
    assertEquals("Zebra Corp", customers.get(2).getCompanyName());
}
```

---

## US-29: List Customers - Controller

**As a developer, I need GET /api/customers endpoint so that customers can be listed via REST API**

**Acceptance Criteria:**
- GET /api/customers endpoint
- Returns 200 OK with customer list
- Returns ApiResponse wrapper
- Integration test with MockMvc

**Implementation Details:**

Add to `CustomerController.java`:

```java
// === LIST ALL ===

@GetMapping
public ResponseEntity<ApiResponse<List<CustomerListItemResponse>>> listAllCustomers() {
    List<CustomerListItemResponse> response = customerService.listAllCustomers();
    return ResponseEntity.ok(ApiResponse.success(response));
}
```

Don't forget the import:
```java
import com.invoiceme.application.customer.dto.CustomerListItemResponse;
import java.util.List;
```

**Testing:**

Add to `CustomerControllerTest.java`:

```java
@Test
void testListAllCustomers() throws Exception {
    // Create test customers
    createCustomerViaAPI("Zebra Corp", "zebra@example.com");
    createCustomerViaAPI("Apple Corp", "apple@example.com");
    createCustomerViaAPI("Microsoft Corp", "microsoft@example.com");

    mockMvc.perform(get("/api/customers")
            .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[0].companyName").value("Apple Corp"))
            .andExpect(jsonPath("$.data[1].companyName").value("Microsoft Corp"))
            .andExpect(jsonPath("$.data[2].companyName").value("Zebra Corp"));
}

@Test
void testListAllCustomersEmpty() throws Exception {
    mockMvc.perform(get("/api/customers")
            .header("Authorization", "Bearer " + authToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(0));
}

private void createCustomerViaAPI(String companyName, String email) throws Exception {
    String requestBody = String.format("""
        {
            "companyName": "%s",
            "email": "%s"
        }
        """, companyName, email);

    mockMvc.perform(post("/api/customers")
            .header("Authorization", "Bearer " + authToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
            .andExpect(status().isCreated());
}
```

---

## US-30: List Customers - Frontend API

**As a frontend developer, I need customerApi.listAll() so that I can fetch customers**

**Acceptance Criteria:**
- customerApi.listAll() method implemented
- Returns array of CustomerListItem

**Implementation Details:**

### 1. Update Customer Model

Add to `src/models/Customer.ts`:

```typescript
export interface CustomerListItem {
  id: string;
  companyName: string;
  email: string | null;
  totalOutstanding: string;
}
```

### 2. Update API Client

Update `src/api/customerApi.ts`:

```typescript
import { apiClient } from './client';
import { ApiResponse } from '../models/ApiResponse';
import { Customer, CustomerListItem, CreateCustomerRequest } from '../models/Customer';

export const customerApi = {
  // GET /api/customers
  listAll: async (): Promise<CustomerListItem[]> => {
    const response = await apiClient.get<ApiResponse<CustomerListItem[]>>('/customers');
    return response.data.data;
  },

  // POST /api/customers
  create: async (request: CreateCustomerRequest): Promise<Customer> => {
    const response = await apiClient.post<ApiResponse<Customer>>('/customers', request);
    return response.data.data;
  },
};
```

---

## US-31: List Customers - Grid View & ViewModel

**As a user, I need to see a list of customers so that I can view all customers**

**Acceptance Criteria:**
- Table showing company name, email, totalOutstanding
- Data loaded from API using React Query
- Loading state shown while fetching
- Error state shown on failure
- "Create Customer" button navigates to form
- Click row navigates to detail view (placeholder for now)

**Implementation Details:**

### 1. Create ViewModel

Create `src/viewmodels/customers/CustomerListViewModel.ts`:

```typescript
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { customerApi } from '../../api/customerApi';
import { CustomerListItem } from '../../models/Customer';

export const CustomerListViewModel = () => {
  const navigate = useNavigate();

  // Query for customer list
  const {
    data: customers,
    isLoading,
    isError,
    error,
  } = useQuery<CustomerListItem[]>({
    queryKey: ['customers', 'list'],
    queryFn: customerApi.listAll,
  });

  // Computed properties
  const errorMessage = isError
    ? error instanceof Error
      ? error.message
      : 'Failed to load customers'
    : null;

  // Actions
  const handleCreateNew = () => {
    navigate('/customers/new');
  };

  const handleRowClick = (customerId: string) => {
    // Will implement detail view in Phase 3C
    console.log('Navigate to customer:', customerId);
  };

  // Expose state and actions to view
  return {
    customers,
    isLoading,
    isError,
    errorMessage,
    handleCreateNew,
    handleRowClick,
  };
};
```

### 2. Update View

Update `src/views/customers/CustomerListView.tsx`:

```typescript
import React from 'react';
import { CustomerListViewModel } from '../../viewmodels/customers/CustomerListViewModel';

export const CustomerListView: React.FC = () => {
  const vm = CustomerListViewModel();

  return (
    <div className="container mx-auto p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold">Customers</h1>
        <button
          onClick={vm.handleCreateNew}
          className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
        >
          Create Customer
        </button>
      </div>

      {vm.isLoading && (
        <div className="text-center py-8">
          <p className="text-gray-600">Loading customers...</p>
        </div>
      )}

      {vm.isError && (
        <div className="bg-red-50 border border-red-300 rounded p-4 my-4">
          <p className="text-red-700">{vm.errorMessage}</p>
        </div>
      )}

      {vm.customers && vm.customers.length === 0 && (
        <div className="text-center py-12 bg-gray-50 rounded">
          <p className="text-gray-600 mb-4">No customers yet</p>
          <button
            onClick={vm.handleCreateNew}
            className="bg-blue-600 text-white px-6 py-2 rounded hover:bg-blue-700"
          >
            Create Your First Customer
          </button>
        </div>
      )}

      {vm.customers && vm.customers.length > 0 && (
        <div className="bg-white rounded-lg shadow overflow-hidden">
          <table className="w-full">
            <thead className="bg-gray-50 border-b">
              <tr>
                <th className="text-left p-4 font-medium text-gray-700">Company Name</th>
                <th className="text-left p-4 font-medium text-gray-700">Email</th>
                <th className="text-right p-4 font-medium text-gray-700">Outstanding</th>
              </tr>
            </thead>
            <tbody>
              {vm.customers.map((customer) => (
                <tr
                  key={customer.id}
                  onClick={() => vm.handleRowClick(customer.id)}
                  className="border-b hover:bg-gray-50 cursor-pointer"
                >
                  <td className="p-4">{customer.companyName}</td>
                  <td className="p-4 text-gray-600">
                    {customer.email || <span className="text-gray-400">—</span>}
                  </td>
                  <td className="p-4 text-right font-mono">
                    ${parseFloat(customer.totalOutstanding).toFixed(2)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};
```

**Manual Testing:**
1. Start backend and frontend
2. Login
3. Navigate to /customers
4. Should see "No customers yet" message
5. Click "Create Your First Customer"
6. Create a customer
7. Should redirect back to /customers
8. Should see customer in table
9. Create 2 more customers
10. Verify all 3 shown sorted by company name
11. Verify outstanding shows $0.00

---

## US-32: List Customers - Integration Test

**As a developer, I need integration test so that I can verify list endpoint works end-to-end**

**Testing:**

The tests in US-29 already cover this. Additional frontend testing would be manual as described in US-31.

---

# Phase 3C-F: Remaining Features

Due to length constraints, I'll provide a summary structure for the remaining phases. Each would follow the same pattern as above.

## Phase 3C: View Customer Detail
- US-33: Get by ID - Repository & Service
- US-34: GET /api/customers/:id - Controller
- US-35: customerApi.getById() - Frontend API
- US-36: CustomerDetailView & ViewModel
- US-37: Add navigation links

## Phase 3D: Update Customer
- US-38: UpdateCustomerRequest DTO
- US-39: CustomerService.updateCustomer()
- US-40: PUT /api/customers/:id Controller
- US-41: customerApi.update() Frontend
- US-42: CustomerFormView edit mode
- US-43: Optimistic lock error handling
- US-44: Integration tests

## Phase 3E: Delete Customer
- US-45: CustomerService.deleteCustomer()
- US-46: DELETE /api/customers/:id Controller
- US-47: customerApi.delete() Frontend
- US-48: ConfirmDialog component
- US-49: Integration tests

## Phase 3F: Polish & Layout
- US-50: TopBar component
- US-51: PageLayout component
- US-52: Loading/Error components

---

## Phase 3 Completion Checklist

**Backend:**
- [ ] Customer entity with full lifecycle methods
- [ ] Customers table created
- [ ] CustomerRepository with all query methods
- [ ] All DTOs created and tested
- [ ] CustomerMapper tested
- [ ] CustomerService with CRUD methods
- [ ] CustomerController with all endpoints
- [ ] Unit tests pass for all layers
- [ ] Integration tests pass

**Frontend:**
- [ ] Customer TypeScript models
- [ ] customerApi with all methods
- [ ] CustomerFormView (create/edit modes)
- [ ] CustomerListView
- [ ] CustomerDetailView
- [ ] TopBar with navigation
- [ ] PageLayout wrapper
- [ ] All views styled with Tailwind
- [ ] Manual tests pass

**Integration:**
- [ ] Can create customer via UI
- [ ] Can list customers via UI
- [ ] Can view customer detail
- [ ] Can update customer
- [ ] Can delete customer
- [ ] Validation errors display correctly
- [ ] Optimistic locking works
- [ ] Soft deletes work

---

## Next Phases

After completing Customer CRUD, the same vertical slice approach would be applied to:
- **Phase 4:** Invoice CRUD with line items
- **Phase 5:** Payment CRUD
- **Phase 6:** Invoice lifecycle (Draft → Sent → Paid)
- **Phase 7:** Polish, performance testing, deployment
