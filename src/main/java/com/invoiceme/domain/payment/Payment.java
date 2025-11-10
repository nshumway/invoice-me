package com.invoiceme.domain.payment;

import com.invoiceme.domain.common.BaseEntity;
import com.invoiceme.domain.common.exceptions.NotFoundException;
import com.invoiceme.domain.common.exceptions.ValidationException;
import com.invoiceme.domain.invoice.Invoice;
import com.invoiceme.domain.invoice.InvoiceStatus;
import com.invoiceme.domain.payment.events.PaymentRecordedEvent;
import jakarta.persistence.*;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    // === Fields ===

    @Column(nullable = false)
    private UUID invoiceId;

    @Column(nullable = false)
    private Instant paymentDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentMethod paymentMethod;

    @Column(length = 255)
    private String referenceNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // === Read-Only Fields (System Managed) ===

    @Column(nullable = false, length = 255)
    private String customerName;

    // === Constructors ===

    public Payment() {
    }

    // === RECORD PAYMENT OPERATION ===

    /**
     * Validates business rules before recording a payment.
     * @param invoiceId The invoice ID from the request
     * @param paymentDate The payment date from the request
     * @param amount The payment amount from the request
     * @param paymentMethod The payment method from the request
     * @param invoice The invoice entity (must be loaded)
     */
    public void beforeCreate(UUID invoiceId, Instant paymentDate, BigDecimal amount,
                            PaymentMethod paymentMethod, Invoice invoice) {
        // Validate invoice exists (already loaded)
        if (invoice == null) {
            throw new NotFoundException("Invoice not found");
        }

        // Validate invoice status is SENT or PAID
        if (invoice.getStatus() != InvoiceStatus.SENT && invoice.getStatus() != InvoiceStatus.PAID) {
            throw new ValidationException("Payments can only be recorded for SENT or PAID invoices");
        }

        // Validate payment date not before invoice date
        if (paymentDate != null && invoice.getInvoiceDate() != null) {
            if (paymentDate.isBefore(invoice.getInvoiceDate())) {
                throw new ValidationException("Payment date cannot be before invoice date");
            }
        }

        // Validate amount > 0
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Payment amount must be greater than 0");
        }

        // Validate payment method provided
        if (paymentMethod == null) {
            throw new ValidationException("Payment method is required");
        }
    }

    /**
     * Records a new payment with the provided data.
     * Call beforeCreate() before this method.
     */
    public void create(UUID invoiceId, Instant paymentDate, BigDecimal amount,
                      PaymentMethod paymentMethod, String referenceNumber, String notes,
                      Invoice invoice) {
        this.invoiceId = invoiceId;
        this.paymentDate = paymentDate;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.referenceNumber = referenceNumber;
        this.notes = notes;

        // Set read-only field
        this.customerName = invoice.getCustomerName();
    }

    /**
     * Publishes domain events after payment creation.
     * Call this after save().
     * @param eventPublisher Spring's event publisher
     */
    public void afterCreate(ApplicationEventPublisher eventPublisher) {
        // Publish domain event: Payment recorded
        eventPublisher.publishEvent(new PaymentRecordedEvent(
            this.getId(),
            this.invoiceId,
            this.amount
        ));
    }

    // === DELETE OPERATION (System Only) ===

    /**
     * Validates business rules before deleting a payment.
     * @param isSystemUpdate True if this is a system update (cascade from invoice delete)
     */
    public void beforeDelete(boolean isSystemUpdate) {
        // No validation needed for system deletes (cascade from invoice delete)
        // User deletes are not allowed at controller level
    }

    /**
     * Soft deletes the payment.
     * Call beforeDelete() before this method.
     */
    public void delete() {
        this.markAsDeleted(); // Soft delete from BaseEntity
    }

    /**
     * Publishes domain events after payment deletion.
     * Call this after save().
     */
    public void afterDelete() {
        // No cascading needed - only called during invoice delete
        // Invoice.amountPaid recalculation not needed since invoice is being deleted
        // No event publishing needed
    }

    // === Getters and Setters ===

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Instant getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Instant paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
}
