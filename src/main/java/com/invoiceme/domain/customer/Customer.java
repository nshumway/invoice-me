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
