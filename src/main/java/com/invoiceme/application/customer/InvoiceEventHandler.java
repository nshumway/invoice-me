package com.invoiceme.application.customer;

import com.invoiceme.domain.common.exceptions.NotFoundException;
import com.invoiceme.domain.common.exceptions.OptimisticLockException;
import com.invoiceme.domain.customer.Customer;
import com.invoiceme.domain.invoice.InvoiceStatus;
import com.invoiceme.domain.invoice.events.InvoiceCreatedEvent;
import com.invoiceme.domain.invoice.events.InvoiceDeletedEvent;
import com.invoiceme.domain.invoice.events.InvoiceStatusChangedEvent;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event handler for invoice events that affect customer statistics.
 * This component listens to invoice domain events and updates customer counters accordingly.
 *
 * All event listeners use @Transactional(propagation = Propagation.MANDATORY) to ensure
 * they participate in the parent transaction for consistency.
 */
@Component
public class InvoiceEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceEventHandler.class);

    @Autowired
    private CustomerRepository customerRepository;

    /**
     * Handle invoice created events.
     * Increments the draft invoice count for the customer.
     * Uses optimistic locking with automatic retry on concurrent modifications.
     * @param event Invoice created event
     */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 50, multiplier = 2.0)
    )
    public void onInvoiceCreated(InvoiceCreatedEvent event) {
        logger.info("Handling InvoiceCreatedEvent: invoiceId={}, customerId={}, status={}",
                   event.getInvoiceId(), event.getCustomerId(), event.getStatus());

        try {
            Customer customer = customerRepository.findByIdAndIsDeletedFalse(event.getCustomerId())
                    .orElseThrow(() -> new NotFoundException("Customer not found: " + event.getCustomerId()));

            // New invoices are always created in DRAFT status
            customer.setDraftInvoiceCount(customer.getDraftInvoiceCount() + 1);
            customerRepository.save(customer);

            logger.debug("Incremented draft invoice count for customer: customerId={}, newCount={}",
                        event.getCustomerId(), customer.getDraftInvoiceCount());
        } catch (ObjectOptimisticLockingFailureException e) {
            logger.warn("Optimistic lock failure handling InvoiceCreatedEvent, will retry: invoiceId={}, customerId={}",
                       event.getInvoiceId(), event.getCustomerId());
            throw e; // Retry will handle this
        } catch (Exception e) {
            logger.error("Error handling InvoiceCreatedEvent: invoiceId={}, customerId={}",
                        event.getInvoiceId(), event.getCustomerId(), e);
            throw e;
        }
    }

    /**
     * Handle invoice status changed events.
     * Updates customer invoice counts and totalOutstanding based on status transitions.
     * Uses optimistic locking with automatic retry on concurrent modifications.
     * @param event Invoice status changed event
     */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 50, multiplier = 2.0)
    )
    public void onInvoiceStatusChanged(InvoiceStatusChangedEvent event) {
        logger.info("Handling InvoiceStatusChangedEvent: invoiceId={}, customerId={}, oldStatus={}, newStatus={}, total={}",
                   event.getInvoiceId(), event.getCustomerId(), event.getOldStatus(),
                   event.getNewStatus(), event.getInvoiceTotal());

        try {
            Customer customer = customerRepository.findByIdAndIsDeletedFalse(event.getCustomerId())
                    .orElseThrow(() -> new NotFoundException("Customer not found: " + event.getCustomerId()));

            // Handle DRAFT → SENT transition
            if (event.getOldStatus() == InvoiceStatus.DRAFT && event.getNewStatus() == InvoiceStatus.SENT) {
                customer.setDraftInvoiceCount(customer.getDraftInvoiceCount() - 1);
                customer.setSentInvoiceCount(customer.getSentInvoiceCount() + 1);
                customer.setTotalOutstanding(customer.getTotalOutstanding().add(event.getInvoiceTotal()));

                logger.debug("Updated customer for DRAFT→SENT: draftCount={}, sentCount={}, totalOutstanding={}",
                            customer.getDraftInvoiceCount(), customer.getSentInvoiceCount(),
                            customer.getTotalOutstanding());
            }

            // Handle SENT → PAID transition (will be implemented in Phase 6)
            if (event.getOldStatus() == InvoiceStatus.SENT && event.getNewStatus() == InvoiceStatus.PAID) {
                customer.setSentInvoiceCount(customer.getSentInvoiceCount() - 1);
                customer.setPaidInvoiceCount(customer.getPaidInvoiceCount() + 1);
                customer.setTotalOutstanding(customer.getTotalOutstanding().subtract(event.getInvoiceTotal()));

                logger.debug("Updated customer for SENT→PAID: sentCount={}, paidCount={}, totalOutstanding={}",
                            customer.getSentInvoiceCount(), customer.getPaidInvoiceCount(),
                            customer.getTotalOutstanding());
            }

            customerRepository.save(customer);
        } catch (ObjectOptimisticLockingFailureException e) {
            logger.warn("Optimistic lock failure handling InvoiceStatusChangedEvent, will retry: invoiceId={}, customerId={}",
                       event.getInvoiceId(), event.getCustomerId());
            throw e; // Retry will handle this
        } catch (Exception e) {
            logger.error("Error handling InvoiceStatusChangedEvent: invoiceId={}, customerId={}",
                        event.getInvoiceId(), event.getCustomerId(), e);
            throw e;
        }
    }

    /**
     * Handle invoice deleted events.
     * Decrements the appropriate invoice count based on the invoice's status.
     * Uses optimistic locking with automatic retry on concurrent modifications.
     * @param event Invoice deleted event
     */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 50, multiplier = 2.0)
    )
    public void onInvoiceDeleted(InvoiceDeletedEvent event) {
        logger.info("Handling InvoiceDeletedEvent: invoiceId={}, customerId={}, status={}",
                   event.getInvoiceId(), event.getCustomerId(), event.getInvoiceStatus());

        try {
            Customer customer = customerRepository.findByIdAndIsDeletedFalse(event.getCustomerId())
                    .orElseThrow(() -> new NotFoundException("Customer not found: " + event.getCustomerId()));

            // Update customer counts based on invoice status
            if (event.getInvoiceStatus() == InvoiceStatus.DRAFT) {
                customer.setDraftInvoiceCount(customer.getDraftInvoiceCount() - 1);
                logger.debug("Decremented draft invoice count: customerId={}, newCount={}",
                            event.getCustomerId(), customer.getDraftInvoiceCount());
            } else if (event.getInvoiceStatus() == InvoiceStatus.PAID) {
                customer.setPaidInvoiceCount(customer.getPaidInvoiceCount() - 1);
                logger.debug("Decremented paid invoice count: customerId={}, newCount={}",
                            event.getCustomerId(), customer.getPaidInvoiceCount());
            }
            // Note: SENT invoices cannot be deleted, so no handling needed

            customerRepository.save(customer);
        } catch (ObjectOptimisticLockingFailureException e) {
            logger.warn("Optimistic lock failure handling InvoiceDeletedEvent, will retry: invoiceId={}, customerId={}",
                       event.getInvoiceId(), event.getCustomerId());
            throw e; // Retry will handle this
        } catch (Exception e) {
            logger.error("Error handling InvoiceDeletedEvent: invoiceId={}, customerId={}",
                        event.getInvoiceId(), event.getCustomerId(), e);
            throw e;
        }
    }
}
