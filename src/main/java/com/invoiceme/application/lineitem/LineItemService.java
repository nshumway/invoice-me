package com.invoiceme.application.lineitem;

import com.invoiceme.application.lineitem.dto.CreateLineItemRequest;
import com.invoiceme.application.lineitem.dto.LineItemResponse;
import com.invoiceme.application.lineitem.dto.UpdateLineItemRequest;
import com.invoiceme.domain.common.exceptions.NotFoundException;
import com.invoiceme.domain.common.exceptions.OptimisticLockException;
import com.invoiceme.domain.invoice.Invoice;
import com.invoiceme.domain.invoice.events.InvoiceCustomerNameChangedEvent;
import com.invoiceme.domain.invoice.events.InvoiceDeletedEvent;
import com.invoiceme.domain.lineitem.LineItem;
import com.invoiceme.infrastructure.persistence.InvoiceRepository;
import com.invoiceme.infrastructure.persistence.LineItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for LineItem operations.
 * Orchestrates domain logic and publishes domain events.
 */
@Service
public class LineItemService {

    private static final Logger logger = LoggerFactory.getLogger(LineItemService.class);

    @Autowired
    private LineItemRepository lineItemRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private LineItemMapper lineItemMapper;

    // === CREATE ===

    /**
     * Creates a new line item and triggers invoice total recalculation.
     * Follows the domain lifecycle: beforeCreate → create → save → afterCreate
     * @param request Create line item request
     * @return Created line item response
     */
    @Transactional
    public LineItemResponse createLineItem(CreateLineItemRequest request) {
        logger.info("Creating line item for invoice: {}", request.getInvoiceId());
        logger.debug("Create line item request: invoiceId={}, description={}",
                    request.getInvoiceId(), request.getDescription());

        try {
            // Load invoice (validates existence and status)
            Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(request.getInvoiceId())
                    .orElseThrow(() -> {
                        logger.warn("Invoice not found for line item creation: invoiceId={}", request.getInvoiceId());
                        return new NotFoundException("Invoice not found");
                    });

            // Create line item
            LineItem lineItem = new LineItem();
            lineItem.beforeCreate(request, invoice);
            lineItem.create(request, invoice);
            lineItemRepository.save(lineItem);
            lineItem.afterCreate(eventPublisher);  // Publishes LineItemChangedEvent

            logger.info("Successfully created line item: id={}, invoiceId={}, lineTotal={}",
                       lineItem.getId(), lineItem.getInvoiceId(), lineItem.getLineTotal());

            return lineItemMapper.toResponse(lineItem);
        } catch (Exception e) {
            logger.error("Error creating line item for invoice: {}", request.getInvoiceId(), e);
            throw e;
        }
    }

    // === LIST ===

    /**
     * Lists all line items for an invoice, ordered by creation date.
     * @param invoiceId Invoice ID
     * @return List of line items
     */
    @Transactional(readOnly = true)
    public List<LineItemResponse> listLineItemsForInvoice(UUID invoiceId) {
        logger.info("Listing line items for invoice: {}", invoiceId);

        try {
            List<LineItemResponse> lineItems = lineItemRepository.findAllByInvoiceIdAndIsDeletedFalseOrderByCreatedAtAsc(invoiceId)
                    .stream()
                    .map(lineItemMapper::toResponse)
                    .collect(Collectors.toList());

            logger.debug("Found {} line items for invoice {}", lineItems.size(), invoiceId);
            return lineItems;
        } catch (Exception e) {
            logger.error("Error listing line items for invoice: {}", invoiceId, e);
            throw e;
        }
    }

    // === UPDATE ===

    /**
     * Updates a line item (user-initiated update).
     * Only allowed for line items on DRAFT invoices.
     * @param request Update request with new values
     * @return Updated line item
     * @throws NotFoundException if line item not found
     * @throws OptimisticLockException if version mismatch
     */
    @Transactional
    public LineItemResponse updateLineItem(UpdateLineItemRequest request) {
        return updateLineItem(request, false);
    }

    /**
     * Updates a line item with optional system update flag.
     * @param request Update request
     * @param isSystemUpdate True if this is a system update (bypass DRAFT check)
     * @return Updated line item
     */
    @Transactional
    public LineItemResponse updateLineItem(UpdateLineItemRequest request, boolean isSystemUpdate) {
        logger.info("Updating line item: id={}, isSystemUpdate={}", request.getId(), isSystemUpdate);
        logger.debug("Update line item request: id={}, version={}", request.getId(), request.getVersion());

        try {
            LineItem lineItem = lineItemRepository.findByIdAndIsDeletedFalse(request.getId())
                    .orElseThrow(() -> {
                        logger.warn("Line item not found for update: id={}", request.getId());
                        return new NotFoundException("Line item not found");
                    });

            // Optimistic locking
            if (!lineItem.getVersion().equals(request.getVersion())) {
                logger.warn("Optimistic lock violation for line item: id={}, expected version={}, actual version={}",
                           request.getId(), request.getVersion(), lineItem.getVersion());
                throw new OptimisticLockException("Line item was modified by another user. Please reload and try again.");
            }

            // Load invoice for validation
            Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(lineItem.getInvoiceId())
                    .orElseThrow(() -> new NotFoundException("Invoice not found"));

            lineItem.beforeUpdate(request, invoice, isSystemUpdate);
            lineItem.update(request);
            lineItemRepository.save(lineItem);
            lineItem.afterUpdate(eventPublisher);  // Publishes LineItemChangedEvent

            logger.info("Successfully updated line item: id={}", lineItem.getId());
            return lineItemMapper.toResponse(lineItem);
        } catch (Exception e) {
            logger.error("Error updating line item: id={}", request.getId(), e);
            throw e;
        }
    }

    // === DELETE ===

    /**
     * Soft-deletes a line item (user-initiated delete).
     * Only allowed for line items on DRAFT invoices.
     * @param lineItemId Line item ID
     * @param version Version for optimistic locking
     * @throws NotFoundException if line item not found
     * @throws OptimisticLockException if version mismatch
     */
    @Transactional
    public void deleteLineItem(UUID lineItemId, Long version) {
        deleteLineItem(lineItemId, version, false);
    }

    /**
     * Soft-deletes a line item with optional system delete flag.
     * @param lineItemId Line item ID
     * @param version Version for optimistic locking
     * @param isSystemUpdate True if this is a system delete (bypass DRAFT check)
     */
    @Transactional
    public void deleteLineItem(UUID lineItemId, Long version, boolean isSystemUpdate) {
        logger.info("Deleting line item: id={}, isSystemUpdate={}", lineItemId, isSystemUpdate);

        try {
            LineItem lineItem = lineItemRepository.findByIdAndIsDeletedFalse(lineItemId)
                    .orElseThrow(() -> {
                        logger.warn("Line item not found for deletion: id={}", lineItemId);
                        return new NotFoundException("Line item not found");
                    });

            // Optimistic locking (skip for system deletes from cascade)
            if (!isSystemUpdate && !lineItem.getVersion().equals(version)) {
                logger.warn("Optimistic lock violation for line item: id={}, expected version={}, actual version={}",
                           lineItemId, version, lineItem.getVersion());
                throw new OptimisticLockException("Line item was modified by another user. Please reload and try again.");
            }

            // Load invoice for validation (null for system cascade deletes)
            Invoice invoice = null;
            if (!isSystemUpdate) {
                invoice = invoiceRepository.findByIdAndIsDeletedFalse(lineItem.getInvoiceId())
                        .orElseThrow(() -> new NotFoundException("Invoice not found"));
            }

            lineItem.beforeDelete(invoice, isSystemUpdate);
            lineItem.delete();
            lineItemRepository.save(lineItem);

            // Only publish event for user deletes (not for cascade deletes from invoice deletion)
            if (!isSystemUpdate) {
                lineItem.afterDelete(eventPublisher);  // Publishes LineItemChangedEvent
            }

            logger.info("Successfully deleted line item: id={}", lineItemId);
        } catch (Exception e) {
            logger.error("Error deleting line item: id={}", lineItemId, e);
            throw e;
        }
    }

    // === EVENT LISTENERS ===

    /**
     * Event listener for invoice customer name changes.
     * Updates denormalized customerName field on all line items for the invoice.
     * This is a system update that participates in the parent transaction.
     * @param event Invoice customer name changed event
     */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onInvoiceCustomerNameChanged(InvoiceCustomerNameChangedEvent event) {
        logger.info("Handling InvoiceCustomerNameChangedEvent: invoiceId={}, newCustomerName={}",
                   event.getInvoiceId(), event.getNewCustomerName());

        try {
            // Update denormalized customerName on all line items for this invoice
            List<LineItem> lineItems = lineItemRepository.findAllByInvoiceIdAndIsDeletedFalseOrderByCreatedAtAsc(event.getInvoiceId());

            logger.debug("Updating {} line items with new customer name", lineItems.size());

            for (LineItem lineItem : lineItems) {
                lineItem.setCustomerName(event.getNewCustomerName());
                lineItemRepository.save(lineItem);
            }

            logger.info("Successfully updated {} line items with new customer name", lineItems.size());
        } catch (Exception e) {
            logger.error("Error handling InvoiceCustomerNameChangedEvent for invoiceId: {}",
                        event.getInvoiceId(), e);
            throw e;
        }
    }

    /**
     * Event listener for invoice deletion.
     * Cascade deletes all line items for the invoice.
     * This is a system delete that participates in the parent transaction.
     * @param event Invoice deleted event
     */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onInvoiceDeleted(InvoiceDeletedEvent event) {
        logger.info("Handling InvoiceDeletedEvent: invoiceId={}", event.getInvoiceId());

        try {
            List<LineItem> lineItems = lineItemRepository.findAllByInvoiceIdAndIsDeletedFalseOrderByCreatedAtAsc(event.getInvoiceId());

            logger.debug("Cascade deleting {} line items for invoice {}", lineItems.size(), event.getInvoiceId());

            for (LineItem lineItem : lineItems) {
                lineItem.beforeDelete(null, true); // isSystemUpdate=true, skip invoice check
                lineItem.delete();
                lineItemRepository.save(lineItem);
                // Note: Don't publish LineItemChangedEvent since invoice is being deleted
            }

            logger.info("Successfully cascade deleted {} line items", lineItems.size());
        } catch (Exception e) {
            logger.error("Error handling InvoiceDeletedEvent for invoiceId: {}", event.getInvoiceId(), e);
            throw e;
        }
    }
}
