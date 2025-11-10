package com.invoiceme.application.payment;

import com.invoiceme.application.payment.dto.PaymentResponse;
import com.invoiceme.application.payment.dto.RecordPaymentRequest;
import com.invoiceme.domain.common.exceptions.NotFoundException;
import com.invoiceme.domain.customer.events.CustomerNameChangedEvent;
import com.invoiceme.domain.invoice.Invoice;
import com.invoiceme.domain.invoice.events.InvoiceDeletedEvent;
import com.invoiceme.domain.payment.Payment;
import com.invoiceme.infrastructure.persistence.InvoiceRepository;
import com.invoiceme.infrastructure.persistence.PaymentRepository;
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
 * Service layer for Payment operations.
 * Orchestrates domain logic and manages event listeners.
 */
@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PaymentMapper paymentMapper;

    // === RECORD PAYMENT ===

    /**
     * Records a new payment for an invoice.
     * Follows the domain lifecycle: beforeCreate → create → save → afterCreate
     * The afterCreate method publishes PaymentRecordedEvent which triggers:
     * - InvoiceService to recalculate amountPaid and check for PAID status transition
     * @param request Record payment request
     * @return Recorded payment response
     */
    @Transactional
    public PaymentResponse recordPayment(RecordPaymentRequest request) {
        logger.info("Recording payment for invoice: {}", request.getInvoiceId());
        logger.debug("Record payment request: invoiceId={}, amount={}, method={}",
                    request.getInvoiceId(), request.getAmount(), request.getPaymentMethod());

        try {
            // Load invoice (validates existence and status)
            Invoice invoice = invoiceRepository.findByIdAndIsDeletedFalse(request.getInvoiceId())
                    .orElseThrow(() -> {
                        logger.warn("Invoice not found for payment: invoiceId={}", request.getInvoiceId());
                        return new NotFoundException("Invoice not found");
                    });

            // Record payment using domain lifecycle
            Payment payment = new Payment();
            payment.beforeCreate(
                request.getInvoiceId(),
                request.getPaymentDate(),
                request.getAmount(),
                request.getPaymentMethod(),
                invoice
            );
            payment.create(
                request.getInvoiceId(),
                request.getPaymentDate(),
                request.getAmount(),
                request.getPaymentMethod(),
                request.getReferenceNumber(),
                request.getNotes(),
                invoice
            );
            paymentRepository.save(payment);
            payment.afterCreate(eventPublisher);  // Publishes PaymentRecordedEvent

            logger.info("Successfully recorded payment: id={}, invoiceId={}, amount={}",
                       payment.getId(), payment.getInvoiceId(), payment.getAmount());

            return paymentMapper.toResponse(payment);
        } catch (Exception e) {
            logger.error("Error recording payment for invoice: {}", request.getInvoiceId(), e);
            throw e;
        }
    }

    // === GET BY ID ===

    /**
     * Gets a single payment by ID.
     * @param id Payment ID
     * @return Payment details
     * @throws NotFoundException if payment not found or soft-deleted
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID id) {
        logger.info("Getting payment by id: {}", id);

        try {
            Payment payment = paymentRepository.findByIdAndIsDeletedFalse(id)
                    .orElseThrow(() -> {
                        logger.warn("Payment not found: id={}", id);
                        return new NotFoundException("Payment not found");
                    });

            logger.debug("Found payment: id={}, invoiceId={}", payment.getId(), payment.getInvoiceId());

            return paymentMapper.toResponse(payment);
        } catch (Exception e) {
            logger.error("Error getting payment by id: {}", id, e);
            throw e;
        }
    }

    // === LIST FOR INVOICE ===

    /**
     * Lists all payments for an invoice.
     * @param invoiceId Invoice ID
     * @return List of payment responses
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> listPaymentsForInvoice(UUID invoiceId) {
        logger.info("Listing payments for invoice: {}", invoiceId);

        try {
            List<Payment> payments = paymentRepository.findAllByInvoiceIdAndIsDeletedFalseOrderByPaymentDateAsc(invoiceId);

            logger.debug("Found {} payments for invoice: {}", payments.size(), invoiceId);

            return payments.stream()
                    .map(paymentMapper::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error listing payments for invoice: {}", invoiceId, e);
            throw e;
        }
    }

    // === EVENT LISTENERS ===

    /**
     * Event listener for InvoiceDeletedEvent.
     * Cascade deletes all payments for the deleted invoice.
     * This participates in the parent transaction.
     * @param event Invoice deleted event
     */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onInvoiceDeleted(InvoiceDeletedEvent event) {
        logger.info("Handling InvoiceDeletedEvent: invoiceId={}, status={}",
                   event.getInvoiceId(), event.getInvoiceStatus());

        try {
            List<Payment> payments = paymentRepository.findAllByInvoiceIdAndIsDeletedFalseOrderByPaymentDateAsc(event.getInvoiceId());

            logger.debug("Cascade deleting {} payments for invoice: {}", payments.size(), event.getInvoiceId());

            for (Payment payment : payments) {
                payment.beforeDelete(true); // isSystemUpdate=true (no validation)
                payment.delete();
                paymentRepository.save(payment);
                payment.afterDelete(); // No-op since invoice is being deleted
            }

            logger.info("Successfully cascade deleted {} payments for invoice: {}", payments.size(), event.getInvoiceId());
        } catch (Exception e) {
            logger.error("Error handling InvoiceDeletedEvent for invoiceId: {}",
                        event.getInvoiceId(), e);
            throw e;
        }
    }

    /**
     * Event listener for CustomerNameChangedEvent.
     * Updates denormalized customerName field on all payments for invoices of this customer.
     * This participates in the parent transaction.
     * @param event Customer name changed event
     */
    @EventListener
    @Transactional(propagation = Propagation.MANDATORY)
    public void onCustomerNameChanged(CustomerNameChangedEvent event) {
        logger.info("Handling CustomerNameChangedEvent: customerId={}, oldName={}, newName={}",
                   event.getCustomerId(), event.getOldCompanyName(), event.getNewCompanyName());

        try {
            // Find all invoices for this customer
            List<Invoice> invoices = invoiceRepository.findAllByCustomerIdAndIsDeletedFalse(event.getCustomerId());

            logger.debug("Updating payments for {} invoices of customer: {}", invoices.size(), event.getCustomerId());

            int totalPaymentsUpdated = 0;

            // For each invoice, update all its payments
            for (Invoice invoice : invoices) {
                List<Payment> payments = paymentRepository.findAllByInvoiceIdAndIsDeletedFalseOrderByPaymentDateAsc(invoice.getId());

                for (Payment payment : payments) {
                    payment.setCustomerName(event.getNewCompanyName());
                    paymentRepository.save(payment);
                    totalPaymentsUpdated++;
                }
            }

            logger.info("Successfully updated {} payments with new customer name for customerId: {}",
                       totalPaymentsUpdated, event.getCustomerId());
        } catch (Exception e) {
            logger.error("Error handling CustomerNameChangedEvent for customerId: {}",
                        event.getCustomerId(), e);
            throw e;
        }
    }
}
