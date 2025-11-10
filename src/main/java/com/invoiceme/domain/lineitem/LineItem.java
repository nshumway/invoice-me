package com.invoiceme.domain.lineitem;

import com.invoiceme.application.lineitem.dto.CreateLineItemRequest;
import com.invoiceme.application.lineitem.dto.UpdateLineItemRequest;
import com.invoiceme.domain.common.BaseEntity;
import com.invoiceme.domain.common.exceptions.NotFoundException;
import com.invoiceme.domain.common.exceptions.ValidationException;
import com.invoiceme.domain.invoice.Invoice;
import com.invoiceme.domain.invoice.InvoiceStatus;
import com.invoiceme.domain.lineitem.events.LineItemChangedEvent;
import com.invoiceme.domain.lineitem.events.LineItemChangeType;
import jakarta.persistence.*;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "line_items")
public class LineItem extends BaseEntity {

    // === User-Editable Fields ===

    @Column(nullable = false)
    private UUID invoiceId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    // === Read-Only Fields (System Managed) ===

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;

    // === Constructors ===

    public LineItem() {
    }

    // === CREATE OPERATION ===

    /**
     * Validates business rules before creating a line item.
     * @param request The create request containing user input
     * @param invoice The invoice entity (must be loaded)
     */
    public void beforeCreate(CreateLineItemRequest request, Invoice invoice) {
        // Validate invoice exists (already loaded)
        if (invoice == null) {
            throw new NotFoundException("Invoice not found");
        }

        // Validate invoice is DRAFT
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new ValidationException("Can only add line items to DRAFT invoices");
        }

        // Validate quantity > 0
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Quantity must be greater than 0");
        }

        // Validate unitPrice > 0
        if (request.getUnitPrice() == null || request.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Unit price must be greater than 0");
        }

        // Validate description not blank
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new ValidationException("Description is required");
        }
    }

    /**
     * Creates a new line item with the provided data.
     * Call beforeCreate() before this method.
     * @param request The create request containing user input
     * @param invoice The invoice entity
     */
    public void create(CreateLineItemRequest request, Invoice invoice) {
        this.invoiceId = request.getInvoiceId();
        this.description = request.getDescription();
        this.quantity = request.getQuantity();
        this.unitPrice = request.getUnitPrice();

        // Set read-only fields
        this.customerName = invoice.getCustomerName();
        this.lineTotal = calculateLineTotal(this.quantity, this.unitPrice);
    }

    /**
     * Publishes domain events after line item creation.
     * Call this after save().
     * @param eventPublisher Spring's event publisher
     */
    public void afterCreate(ApplicationEventPublisher eventPublisher) {
        // Publish domain event: Line item changed
        eventPublisher.publishEvent(new LineItemChangedEvent(
            this.invoiceId,
            this.getId(),
            LineItemChangeType.CREATED
        ));
    }

    // === UPDATE OPERATION ===

    /**
     * Validates business rules before updating a line item.
     * @param request The update request
     * @param invoice The invoice entity
     * @param isSystemUpdate True if this is a system update (cascading from another domain)
     */
    public void beforeUpdate(UpdateLineItemRequest request,
                            Invoice invoice,
                            boolean isSystemUpdate) {
        // If user update, must be DRAFT
        if (!isSystemUpdate && invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new ValidationException("Can only edit line items on DRAFT invoices");
        }

        // Validate quantity > 0
        if (request.getQuantity() == null || request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Quantity must be greater than 0");
        }

        // Validate unitPrice > 0
        if (request.getUnitPrice() == null || request.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Unit price must be greater than 0");
        }

        // Validate description not blank
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new ValidationException("Description is required");
        }
    }

    /**
     * Updates line item fields.
     * Call beforeUpdate() before this method.
     * @param request The update request
     */
    public void update(UpdateLineItemRequest request) {
        this.description = request.getDescription();
        this.quantity = request.getQuantity();
        this.unitPrice = request.getUnitPrice();

        // Recalculate lineTotal
        this.lineTotal = calculateLineTotal(this.quantity, this.unitPrice);
    }

    /**
     * Publishes domain events after line item update.
     * Call this after save().
     * @param eventPublisher Spring's event publisher
     */
    public void afterUpdate(ApplicationEventPublisher eventPublisher) {
        // Publish domain event: Line item changed
        eventPublisher.publishEvent(new LineItemChangedEvent(
            this.invoiceId,
            this.getId(),
            LineItemChangeType.UPDATED
        ));
    }

    // === DELETE OPERATION ===

    /**
     * Validates business rules before deleting a line item.
     * @param invoice The invoice entity
     * @param isSystemUpdate True if this is a system delete (cascading from invoice deletion)
     */
    public void beforeDelete(Invoice invoice, boolean isSystemUpdate) {
        // If user delete, must be DRAFT
        if (!isSystemUpdate && invoice != null && invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new ValidationException("Can only delete line items from DRAFT invoices");
        }
    }

    /**
     * Soft deletes the line item.
     * Call beforeDelete() before this method.
     */
    public void delete() {
        this.markAsDeleted(); // Soft delete from BaseEntity
    }

    /**
     * Publishes domain events after line item deletion.
     * Call this after save().
     * @param eventPublisher Spring's event publisher
     */
    public void afterDelete(ApplicationEventPublisher eventPublisher) {
        // Publish domain event: Line item changed
        eventPublisher.publishEvent(new LineItemChangedEvent(
            this.invoiceId,
            this.getId(),
            LineItemChangeType.DELETED
        ));
    }

    // === Helper Methods ===

    /**
     * Calculates line total: quantity × unitPrice
     * @param quantity Line item quantity
     * @param unitPrice Unit price
     * @return Line total rounded to 2 decimal places
     */
    private BigDecimal calculateLineTotal(BigDecimal quantity, BigDecimal unitPrice) {
        return quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }

    // === Getters and Setters ===

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}
