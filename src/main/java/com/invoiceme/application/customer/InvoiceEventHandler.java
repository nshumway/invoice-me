package com.invoiceme.application.customer;

import com.invoiceme.domain.common.exceptions.NotFoundException;
import com.invoiceme.domain.common.exceptions.OptimisticLockException;
import com.invoiceme.domain.customer.Customer;
import com.invoiceme.domain.invoice.Invoice;
import com.invoiceme.domain.invoice.InvoiceStatus;
import com.invoiceme.domain.invoice.events.InvoiceCreatedEvent;
import com.invoiceme.domain.invoice.events.InvoiceDeletedEvent;
import com.invoiceme.domain.invoice.events.InvoiceStatusChangedEvent;
import com.invoiceme.domain.payment.events.PaymentRecordedEvent;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
import com.invoiceme.infrastructure.persistence.InvoiceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

import java.math.BigDecimal;

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

    @Autowired
    private InvoiceRepository invoiceRepository;

    @PersistenceContext
    private EntityManager entityManager;

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
            // Recalculate invoice counts from database
            recalculateCustomerInvoiceCounts(customer);
            customerRepository.save(customer);

            logger.debug("Recalculated draft invoice count for customer: customerId={}, newCount={}",
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
                // Recalculate invoice counts from database
                recalculateCustomerInvoiceCounts(customer);

                // Recalculate totalOutstanding from database
                recalculateTotalOutstanding(customer);

                logger.debug("Updated customer for DRAFT→SENT: draftCount={}, sentCount={}, totalOutstanding={}",
                            customer.getDraftInvoiceCount(), customer.getSentInvoiceCount(),
                            customer.getTotalOutstanding());
            }

            // Handle SENT → PAID transition
            if (event.getOldStatus() == InvoiceStatus.SENT && event.getNewStatus() == InvoiceStatus.PAID) {
                // Recalculate invoice counts from database
                recalculateCustomerInvoiceCounts(customer);

                // Recalculate totalOutstanding from database
                recalculateTotalOutstanding(customer);

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

            // Recalculate invoice counts from database
            recalculateCustomerInvoiceCounts(customer);
            // Note: SENT invoices cannot be deleted, so no handling needed

            logger.debug("Recalculated invoice counts after deletion: customerId={}, draftCount={}, paidCount={}",
                        event.getCustomerId(), customer.getDraftInvoiceCount(), customer.getPaidInvoiceCount());

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

    /**
     * Handle payment recorded events.
     * Recalculates customer totalOutstanding from database.
     * This ensures partial payments immediately update the customer's outstanding balance.
     * Uses optimistic locking with automatic retry on concurrent modifications.
     * Order(2) ensures this runs after InvoiceService updates invoice.amountPaid.
     * @param event Payment recorded event
     */
    @EventListener
    @org.springframework.core.annotation.Order(2)
    @Transactional(propagation = Propagation.MANDATORY)
    @Retryable(
        retryFor = {ObjectOptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 50, multiplier = 2.0)
    )
    public void onPaymentRecorded(PaymentRecordedEvent event) {
        logger.info("Handling PaymentRecordedEvent: paymentId={}, invoiceId={}, amount={}",
                   event.getPaymentId(), event.getInvoiceId(), event.getAmount());

        try {
            // Flush pending changes (invoice.amountPaid update) before recalculating
            entityManager.flush();

            // Load the invoice to get the customer ID
            Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(event.getInvoiceId())
                    .orElseThrow(() -> new NotFoundException("Invoice not found: " + event.getInvoiceId()));

            Customer customer = customerRepository.findByIdAndIsDeletedFalse(invoice.getCustomerId())
                    .orElseThrow(() -> new NotFoundException("Customer not found: " + invoice.getCustomerId()));

            // Recalculate totalOutstanding from database
            recalculateTotalOutstanding(customer);

            customerRepository.save(customer);

            logger.debug("Recalculated customer totalOutstanding after payment: customerId={}, paymentAmount={}, newOutstanding={}",
                        customer.getId(), event.getAmount(), customer.getTotalOutstanding());
        } catch (ObjectOptimisticLockingFailureException e) {
            logger.warn("Optimistic lock failure handling PaymentRecordedEvent, will retry: paymentId={}, invoiceId={}",
                       event.getPaymentId(), event.getInvoiceId());
            throw e; // Retry will handle this
        } catch (Exception e) {
            logger.error("Error handling PaymentRecordedEvent: paymentId={}, invoiceId={}",
                        event.getPaymentId(), event.getInvoiceId(), e);
            throw e;
        }
    }

    /**
     * Recalculates customer invoice counts from database.
     * @param customer The customer to update
     */
    private void recalculateCustomerInvoiceCounts(Customer customer) {
        Integer draftCount = invoiceRepository.countByCustomerIdAndStatusAndIsDeletedFalse(
            customer.getId(), InvoiceStatus.DRAFT);
        Integer sentCount = invoiceRepository.countByCustomerIdAndStatusAndIsDeletedFalse(
            customer.getId(), InvoiceStatus.SENT);
        Integer paidCount = invoiceRepository.countByCustomerIdAndStatusAndIsDeletedFalse(
            customer.getId(), InvoiceStatus.PAID);

        customer.setDraftInvoiceCount(draftCount != null ? draftCount : 0);
        customer.setSentInvoiceCount(sentCount != null ? sentCount : 0);
        customer.setPaidInvoiceCount(paidCount != null ? paidCount : 0);
    }

    /**
     * Recalculates customer totalOutstanding from database.
     * Sums the balance (total - amountPaid) of all SENT and PAID invoices.
     * @param customer The customer to update
     */
    private void recalculateTotalOutstanding(Customer customer) {
        BigDecimal totalOutstanding = invoiceRepository.calculateTotalOutstanding(customer.getId());
        customer.setTotalOutstanding(totalOutstanding != null ? totalOutstanding : BigDecimal.ZERO);
    }
}
