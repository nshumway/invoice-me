package com.invoiceme.application.customer;

import com.invoiceme.domain.customer.events.CustomerDeletedEvent;
import com.invoiceme.domain.customer.events.CustomerNameChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event handler for customer domain events.
 *
 * This demonstrates the domain events pattern for decoupling domains.
 * In Phase 4-6, the Invoice domain will have its own event handler
 * (e.g., InvoiceCustomerEventHandler) that listens to these events
 * and updates invoices accordingly.
 *
 * Using @TransactionalEventListener ensures events are only processed
 * after the transaction commits, preventing issues if the transaction rolls back.
 */
@Component
public class CustomerEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomerEventHandler.class);

    /**
     * Handle customer name changes.
     * In Phase 4-6, this will be implemented in the Invoice domain to update customer names on invoices.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCustomerNameChanged(CustomerNameChangedEvent event) {
        logger.info("Customer name changed event received: {}", event);

        // TODO (Phase 4-6): Implement in Invoice domain
        // Example implementation in InvoiceCustomerEventHandler:
        // List<Invoice> invoices = invoiceRepository.findByCustomerId(event.getCustomerId());
        // for (Invoice invoice : invoices) {
        //     invoice.updateCustomerName(event.getNewCompanyName());
        //     invoiceRepository.save(invoice);
        // }
    }

    /**
     * Handle customer deletion.
     * In Phase 4-6, this will be implemented in the Invoice domain to cascade delete invoices.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCustomerDeleted(CustomerDeletedEvent event) {
        logger.info("Customer deleted event received: {}", event);

        // TODO (Phase 4-6): Implement in Invoice domain
        // Example implementation in InvoiceCustomerEventHandler:
        // 1. Check for SENT invoices (should have been validated before delete)
        // 2. Soft delete all DRAFT and PAID invoices for this customer
        // List<Invoice> invoices = invoiceRepository.findByCustomerId(event.getCustomerId());
        // for (Invoice invoice : invoices) {
        //     if (invoice.getStatus() != InvoiceStatus.SENT) {
        //         invoice.delete();
        //         invoiceRepository.save(invoice);
        //     }
        // }
    }
}
