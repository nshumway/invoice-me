package com.invoiceme.domain.invoice;

import com.invoiceme.application.invoice.dto.CreateInvoiceRequest;
import com.invoiceme.application.invoice.dto.UpdateInvoiceRequest;
import com.invoiceme.domain.common.BaseEntity;
import com.invoiceme.domain.common.exceptions.NotFoundException;
import com.invoiceme.domain.common.exceptions.ValidationException;
import com.invoiceme.domain.customer.Customer;
import com.invoiceme.domain.invoice.events.InvoiceCreatedEvent;
import com.invoiceme.domain.invoice.events.InvoiceDeletedEvent;
import com.invoiceme.domain.invoice.events.InvoiceStatusChangedEvent;
import com.invoiceme.infrastructure.persistence.InvoiceRepository;
import jakarta.persistence.*;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoices")
public class Invoice extends BaseEntity {

    // === User-Editable Fields ===

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false, length = 50)
    private String invoiceNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // === Read-Only Fields (System Managed) ===

    private Instant invoiceDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    // === Constructors ===

    public Invoice() {
    }

    // === CREATE OPERATION ===

    /**
     * Validates business rules before creating an invoice.
     * @param request The create request containing user input
     * @param customer The customer entity (must be loaded)
     * @param invoiceRepository Repository for checking invoice number uniqueness
     */
    public void beforeCreate(CreateInvoiceRequest request,
                            Customer customer,
                            InvoiceRepository invoiceRepository) {
        // Validate customer exists (should already be loaded by service)
        if (customer == null) {
            throw new NotFoundException("Customer not found");
        }

        // Validate invoice number uniqueness if provided
        if (request.getInvoiceNumber() != null && !request.getInvoiceNumber().isBlank()) {
            if (invoiceRepository.existsByInvoiceNumberAndIsDeletedFalse(request.getInvoiceNumber())) {
                throw new ValidationException("Invoice number already exists: " + request.getInvoiceNumber());
            }
        }
    }

    /**
     * Creates a new invoice with the provided data.
     * Call beforeCreate() before this method.
     * @param request The create request containing user input
     * @param customer The customer entity
     * @param invoiceRepository Repository for generating invoice number
     */
    public void create(CreateInvoiceRequest request, Customer customer, InvoiceRepository invoiceRepository) {
        this.customerId = request.getCustomerId();
        this.notes = request.getNotes();

        // Generate invoice number if not provided
        if (request.getInvoiceNumber() != null && !request.getInvoiceNumber().isBlank()) {
            this.invoiceNumber = request.getInvoiceNumber();
        } else {
            this.invoiceNumber = generateInvoiceNumber(invoiceRepository);
        }

        // Set read-only fields
        this.status = InvoiceStatus.DRAFT;
        this.customerName = customer.getCompanyName();
        this.total = BigDecimal.ZERO;
        this.amountPaid = BigDecimal.ZERO;
    }

    /**
     * Publishes domain events after invoice creation.
     * Call this after save().
     * @param eventPublisher Spring's event publisher
     */
    public void afterCreate(ApplicationEventPublisher eventPublisher) {
        // Publish domain event: Invoice created
        eventPublisher.publishEvent(new InvoiceCreatedEvent(
            this.getId(),
            this.customerId,
            this.status
        ));
    }

    /**
     * Generates invoice number in format: INV-YYYY-MM-DD-###
     * Sequence resets daily.
     * @param invoiceRepository Repository for querying existing numbers
     * @return Generated invoice number
     */
    private String generateInvoiceNumber(InvoiceRepository invoiceRepository) {
        LocalDate today = LocalDate.now();
        String datePrefix = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String invoicePrefix = "INV-" + datePrefix + "-";

        // Find max sequence number for today
        List<String> todaysInvoices = invoiceRepository.findInvoiceNumbersByPrefix(invoicePrefix);

        int maxSequence = 0;
        for (String invoiceNum : todaysInvoices) {
            try {
                String sequencePart = invoiceNum.substring(invoicePrefix.length());
                int sequence = Integer.parseInt(sequencePart);
                if (sequence > maxSequence) {
                    maxSequence = sequence;
                }
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                // Ignore malformed invoice numbers
            }
        }

        int nextSequence = maxSequence + 1;
        return String.format("%s%03d", invoicePrefix, nextSequence);
    }

    // === UPDATE OPERATION ===

    /**
     * Validates business rules before updating an invoice.
     * @param request The update request
     * @param invoiceRepository Repository for checking invoice number uniqueness
     * @param isSystemUpdate True if this is a system update (cascading from another domain)
     */
    public void beforeUpdate(UpdateInvoiceRequest request,
                            InvoiceRepository invoiceRepository,
                            boolean isSystemUpdate) {
        // If user update, must be DRAFT
        if (!isSystemUpdate && this.status != InvoiceStatus.DRAFT) {
            throw new ValidationException("Cannot edit invoice with status " + this.status + ". Only DRAFT invoices can be edited.");
        }

        // Validate invoice number uniqueness if changed
        if (request.getInvoiceNumber() != null &&
            !request.getInvoiceNumber().equals(this.invoiceNumber)) {
            if (invoiceRepository.existsByInvoiceNumberAndIsDeletedFalse(request.getInvoiceNumber())) {
                throw new ValidationException("Invoice number already exists: " + request.getInvoiceNumber());
            }
        }
    }

    /**
     * Updates invoice fields.
     * Call beforeUpdate() before this method.
     * @param request The update request
     */
    public void update(UpdateInvoiceRequest request) {
        // Update editable fields
        if (request.getInvoiceNumber() != null) {
            this.invoiceNumber = request.getInvoiceNumber();
        }
        if (request.getNotes() != null) {
            this.notes = request.getNotes();
        }

        // Note: customerId is NOT editable
    }

    /**
     * Publishes domain events after invoice update.
     * Call this after save().
     * @param eventPublisher Spring's event publisher
     */
    public void afterUpdate(ApplicationEventPublisher eventPublisher) {
        // No domain events needed for invoice updates
        // Customer name changes are handled via CustomerNameChangedEvent from Customer domain
    }

    // === MARK AS SENT OPERATION ===

    /**
     * Validates business rules before marking invoice as sent.
     */
    public void beforeSend() {
        if (this.status != InvoiceStatus.DRAFT) {
            throw new ValidationException("Only DRAFT invoices can be sent. Current status: " + this.status);
        }

        if (this.total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Cannot send invoice with total of $0.00");
        }
    }

    /**
     * Marks the invoice as sent.
     * Call beforeSend() before this method.
     */
    public void send() {
        InvoiceStatus oldStatus = this.status;
        this.status = InvoiceStatus.SENT;
        this.invoiceDate = Instant.now();
    }

    /**
     * Publishes domain events after marking as sent.
     * Call this after save().
     * @param eventPublisher Spring's event publisher
     */
    public void afterSend(ApplicationEventPublisher eventPublisher) {
        // Publish domain event: Invoice status changed DRAFT → SENT
        eventPublisher.publishEvent(new InvoiceStatusChangedEvent(
            this.getId(),
            this.customerId,
            InvoiceStatus.DRAFT,  // oldStatus
            InvoiceStatus.SENT,   // newStatus
            this.total
        ));
    }

    // === DELETE OPERATION ===

    /**
     * Validates business rules before deleting an invoice.
     */
    public void beforeDelete() {
        if (this.status == InvoiceStatus.SENT) {
            throw new ValidationException("Cannot delete SENT invoices. Only DRAFT and PAID invoices can be deleted.");
        }
    }

    /**
     * Soft deletes the invoice.
     * Call beforeDelete() before this method.
     */
    public void delete() {
        this.markAsDeleted(); // Soft delete from BaseEntity
    }

    /**
     * Publishes domain events after invoice deletion.
     * Call this after save().
     * @param eventPublisher Spring's event publisher
     */
    public void afterDelete(ApplicationEventPublisher eventPublisher) {
        // Publish domain event: Invoice deleted
        eventPublisher.publishEvent(new InvoiceDeletedEvent(
            this.getId(),
            this.customerId,
            this.status
        ));
    }

    // === Getters and Setters ===

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getInvoiceDate() {
        return invoiceDate;
    }

    public void setInvoiceDate(Instant invoiceDate) {
        this.invoiceDate = invoiceDate;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    /**
     * Calculates the outstanding balance.
     * This is a calculated field, not stored in the database.
     * @return total - amountPaid
     */
    public BigDecimal getBalance() {
        return this.total.subtract(this.amountPaid);
    }
}
