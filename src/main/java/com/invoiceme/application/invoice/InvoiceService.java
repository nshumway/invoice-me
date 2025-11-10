package com.invoiceme.application.invoice;

import com.invoiceme.application.invoice.dto.*;
import com.invoiceme.domain.common.exceptions.NotFoundException;
import com.invoiceme.domain.common.exceptions.OptimisticLockException;
import com.invoiceme.domain.customer.Customer;
import com.invoiceme.domain.customer.events.CustomerNameChangedEvent;
import com.invoiceme.domain.invoice.Invoice;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
import com.invoiceme.infrastructure.persistence.InvoiceRepository;
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
 * Service layer for Invoice operations.
 * Orchestrates domain logic and publishes domain events.
 */
@Service
public class InvoiceService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private InvoiceMapper invoiceMapper;

    // === CREATE ===

    /**
     * Creates a new invoice in DRAFT status.
     * Follows the domain lifecycle: beforeCreate → create → save → afterCreate
     * @param request Create invoice request
     * @return Created invoice response
     */
    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        logger.info("Creating invoice for customer: {}", request.getCustomerId());
        logger.debug("Create invoice request: customerId={}, invoiceNumber={}",
                    request.getCustomerId(), request.getInvoiceNumber());

        try {
            // Load customer (validates existence)
            Customer customer = customerRepository.findByIdAndIsDeletedFalse(request.getCustomerId())
                    .orElseThrow(() -> {
                        logger.warn("Customer not found for invoice creation: customerId={}", request.getCustomerId());
                        return new NotFoundException("Customer not found");
                    });

            // Create invoice using domain lifecycle
            Invoice invoice = new Invoice();
            invoice.beforeCreate(request, customer, invoiceRepository);
            invoice.create(request, customer, invoiceRepository);
            invoiceRepository.save(invoice);
            invoice.afterCreate(eventPublisher);  // Publishes InvoiceCreatedEvent

            logger.info("Successfully created invoice: id={}, invoiceNumber={}, customerId={}",
                       invoice.getId(), invoice.getInvoiceNumber(), invoice.getCustomerId());

            return invoiceMapper.toResponse(invoice);
        } catch (Exception e) {
            logger.error("Error creating invoice for customer: {}", request.getCustomerId(), e);
            throw e;
        }
    }

    // === GET BY ID ===

    /**
     * Gets a single invoice by ID.
     * @param id Invoice ID
     * @return Invoice details
     * @throws NotFoundException if invoice not found or soft-deleted
     */
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(UUID id) {
        logger.info("Getting invoice by id: {}", id);

        try {
            Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(id)
                    .orElseThrow(() -> {
                        logger.warn("Invoice not found: id={}", id);
                        return new NotFoundException("Invoice not found");
                    });

            return invoiceMapper.toResponse(invoice);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error getting invoice by id: {}", id, e);
            throw e;
        }
    }

    // === UPDATE ===

    /**
     * Updates an invoice (user-initiated update).
     * Only allowed for DRAFT invoices.
     * @param request Update request with new values
     * @return Updated invoice
     * @throws NotFoundException if invoice not found
     * @throws OptimisticLockException if version mismatch
     * @throws ValidationException if trying to update non-DRAFT invoice
     */
    @Transactional
    public InvoiceResponse updateInvoice(UpdateInvoiceRequest request) {
        logger.info("Updating invoice: id={}", request.getId());
        logger.debug("Update invoice request: id={}, version={}", request.getId(), request.getVersion());

        try {
            Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(request.getId())
                    .orElseThrow(() -> {
                        logger.warn("Invoice not found for update: id={}", request.getId());
                        return new NotFoundException("Invoice not found");
                    });

            // Optimistic locking check
            if (!invoice.getVersion().equals(request.getVersion())) {
                logger.warn("Optimistic lock violation for invoice: id={}, expected version={}, actual version={}",
                           request.getId(), request.getVersion(), invoice.getVersion());
                throw new OptimisticLockException(
                    "Invoice was modified by another user. Please reload and try again.");
            }

            // Perform update (user update, not system update)
            invoice.beforeUpdate(request, invoiceRepository, false);
            invoice.update(request);
            invoiceRepository.save(invoice);
            invoice.afterUpdate(eventPublisher);

            logger.info("Successfully updated invoice: id={}", invoice.getId());
            return invoiceMapper.toResponse(invoice);
        } catch (Exception e) {
            logger.error("Error updating invoice: id={}", request.getId(), e);
            throw e;
        }
    }

    // === SEND ===

    /**
     * Marks an invoice as SENT.
     * Transitions from DRAFT → SENT status.
     * Sets invoice_date to current timestamp.
     * @param invoiceId Invoice ID
     * @return Updated invoice
     * @throws NotFoundException if invoice not found
     * @throws ValidationException if invoice is not in DRAFT status
     */
    @Transactional
    public InvoiceResponse markInvoiceAsSent(UUID invoiceId) {
        logger.info("Marking invoice as sent: id={}", invoiceId);

        try {
            Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(invoiceId)
                    .orElseThrow(() -> {
                        logger.warn("Invoice not found for sending: id={}", invoiceId);
                        return new NotFoundException("Invoice not found");
                    });

            // Perform send operation
            invoice.beforeSend();
            invoice.send();
            invoiceRepository.save(invoice);
            invoice.afterSend(eventPublisher);

            logger.info("Successfully marked invoice as sent: id={}, invoiceNumber={}",
                       invoice.getId(), invoice.getInvoiceNumber());
            return invoiceMapper.toResponse(invoice);
        } catch (Exception e) {
            logger.error("Error marking invoice as sent: id={}", invoiceId, e);
            throw e;
        }
    }

    // === LIST ===

    /**
     * Lists all invoices, sorted by invoice date descending (most recent first).
     * Excludes soft-deleted invoices.
     * @return List of invoice summaries
     */
    @Transactional(readOnly = true)
    public List<InvoiceListItemResponse> listAllInvoices() {
        logger.info("Listing all invoices");

        try {
            List<InvoiceListItemResponse> invoices = invoiceRepository.findAllByIsDeletedFalseOrderByInvoiceDateDesc()
                    .stream()
                    .map(invoiceMapper::toListItem)
                    .collect(Collectors.toList());

            logger.debug("Found {} invoices", invoices.size());
            return invoices;
        } catch (Exception e) {
            logger.error("Error listing all invoices", e);
            throw e;
        }
    }

    /**
     * Lists all invoices for a specific customer.
     * @param customerId Customer ID to filter by
     * @return List of invoice summaries for the customer
     */
    @Transactional(readOnly = true)
    public List<InvoiceListItemResponse> listInvoicesByCustomer(UUID customerId) {
        logger.info("Listing invoices for customer: {}", customerId);

        try {
            List<InvoiceListItemResponse> invoices = invoiceRepository.findAllByCustomerIdAndIsDeletedFalse(customerId)
                    .stream()
                    .map(invoiceMapper::toListItem)
                    .collect(Collectors.toList());

            logger.debug("Found {} invoices for customer {}", invoices.size(), customerId);
            return invoices;
        } catch (Exception e) {
            logger.error("Error listing invoices for customer: {}", customerId, e);
            throw e;
        }
    }

    // === DELETE ===

    /**
     * Soft-deletes an invoice.
     * Only DRAFT invoices can be deleted (not SENT or PAID).
     * @param invoiceId Invoice ID
     * @throws NotFoundException if invoice not found
     * @throws ValidationException if invoice is not in DRAFT status
     */
    @Transactional
    public void deleteInvoice(UUID invoiceId) {
        logger.info("Deleting invoice: id={}", invoiceId);

        try {
            Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(invoiceId)
                    .orElseThrow(() -> {
                        logger.warn("Invoice not found for deletion: id={}", invoiceId);
                        return new NotFoundException("Invoice not found");
                    });

            // Perform delete operation
            invoice.beforeDelete();
            invoice.delete();
            invoiceRepository.save(invoice);
            invoice.afterDelete(eventPublisher);

            logger.info("Successfully deleted invoice: id={}, invoiceNumber={}",
                       invoice.getId(), invoice.getInvoiceNumber());
        } catch (Exception e) {
            logger.error("Error deleting invoice: id={}", invoiceId, e);
            throw e;
        }
    }

    // === EVENT LISTENERS ===

    /**
     * Event listener for customer name changes.
     * Updates denormalized customerName field on all invoices for the customer.
     * This is a system update that participates in the parent transaction.
     * @param event Customer name changed event
     */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onCustomerNameChanged(CustomerNameChangedEvent event) {
        logger.info("Handling CustomerNameChangedEvent: customerId={}, oldName={}, newName={}",
                   event.getCustomerId(), event.getOldCompanyName(), event.getNewCompanyName());

        try {
            // Update denormalized customerName on all invoices for this customer
            List<Invoice> invoices = invoiceRepository.findAllByCustomerIdAndIsDeletedFalse(event.getCustomerId());

            logger.debug("Updating {} invoices with new customer name", invoices.size());

            for (Invoice invoice : invoices) {
                invoice.setCustomerName(event.getNewCompanyName());
                invoiceRepository.save(invoice);
            }

            logger.info("Successfully updated {} invoices with new customer name", invoices.size());
        } catch (Exception e) {
            logger.error("Error handling CustomerNameChangedEvent for customerId: {}",
                        event.getCustomerId(), e);
            throw e;
        }
    }

    // === SYSTEM UPDATE METHODS (for backward compatibility with Customer domain) ===

    /**
     * System update method for invoice updates (used by Customer domain cascades).
     * This is kept for backward compatibility with existing Customer domain code.
     * @deprecated Use event listeners instead
     */
    @Deprecated
    @Transactional(propagation = Propagation.MANDATORY)
    public InvoiceResponse systemUpdateInvoice(UpdateInvoiceRequest request) {
        logger.warn("Using deprecated systemUpdateInvoice method. Consider migrating to event-driven approach.");

        Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(request.getId())
                .orElseThrow(() -> new NotFoundException("Invoice not found"));

        // System updates bypass DRAFT status check
        invoice.beforeUpdate(request, invoiceRepository, true);
        invoice.update(request);
        invoiceRepository.save(invoice);
        invoice.afterUpdate(eventPublisher);

        return invoiceMapper.toResponse(invoice);
    }

    /**
     * System delete method for invoices (used by Customer domain cascades).
     * This is kept for backward compatibility with existing Customer domain code.
     * @deprecated Use event listeners instead
     */
    @Deprecated
    @Transactional(propagation = Propagation.MANDATORY)
    public void systemDeleteInvoice(UUID invoiceId) {
        logger.warn("Using deprecated systemDeleteInvoice method. Consider migrating to event-driven approach.");

        Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));

        // System deletes bypass SENT status check
        invoice.delete();
        invoiceRepository.save(invoice);
        // Note: Not calling afterDelete to avoid cascading events back to customer
    }
}
