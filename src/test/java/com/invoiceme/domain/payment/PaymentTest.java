package com.invoiceme.domain.payment;

import com.invoiceme.application.common.UserContext;
import com.invoiceme.domain.common.exceptions.NotFoundException;
import com.invoiceme.domain.common.exceptions.ValidationException;
import com.invoiceme.domain.invoice.Invoice;
import com.invoiceme.domain.invoice.InvoiceStatus;
import com.invoiceme.domain.payment.events.PaymentRecordedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Payment entity domain logic.
 * These tests focus on pure domain behavior with minimal infrastructure dependencies.
 */
class PaymentTest {

    private UUID testUserId;
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        UserContext.setCurrentUser(testUserId);
        eventPublisher = mock(ApplicationEventPublisher.class);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // === CREATE TESTS ===

    @Test
    void testRecordPaymentSuccess() {
        // Given
        Invoice invoice = createSentInvoice();
        UUID invoiceId = invoice.getId();
        Instant paymentDate = Instant.now();
        BigDecimal amount = new BigDecimal("150.00");
        PaymentMethod paymentMethod = PaymentMethod.CASH;
        String referenceNumber = "CHK-12345";
        String notes = "Payment received in full";

        // When
        Payment payment = new Payment();
        payment.beforeCreate(invoiceId, paymentDate, amount, paymentMethod, invoice);
        payment.create(invoiceId, paymentDate, amount, paymentMethod, referenceNumber, notes, invoice);

        // Then
        assertEquals(invoiceId, payment.getInvoiceId());
        assertEquals(paymentDate, payment.getPaymentDate());
        assertEquals(amount, payment.getAmount());
        assertEquals(paymentMethod, payment.getPaymentMethod());
        assertEquals(referenceNumber, payment.getReferenceNumber());
        assertEquals(notes, payment.getNotes());
        assertEquals(invoice.getCustomerName(), payment.getCustomerName());
    }

    @Test
    void testRecordPaymentWithMinimalFields() {
        // Given
        Invoice invoice = createSentInvoice();
        UUID invoiceId = invoice.getId();
        Instant paymentDate = Instant.now();
        BigDecimal amount = new BigDecimal("100.00");
        PaymentMethod paymentMethod = PaymentMethod.CREDIT_CARD;

        // When
        Payment payment = new Payment();
        payment.beforeCreate(invoiceId, paymentDate, amount, paymentMethod, invoice);
        payment.create(invoiceId, paymentDate, amount, paymentMethod, null, null, invoice);

        // Then
        assertNull(payment.getReferenceNumber());
        assertNull(payment.getNotes());
        assertEquals(amount, payment.getAmount());
    }

    @Test
    void testAfterCreatePublishesEvent() {
        // Given
        Invoice invoice = createSentInvoice();
        Payment payment = new Payment();
        payment.beforeCreate(invoice.getId(), Instant.now(), new BigDecimal("100.00"), PaymentMethod.CASH, invoice);
        payment.create(invoice.getId(), Instant.now(), new BigDecimal("100.00"), PaymentMethod.CASH, null, null, invoice);

        // When
        payment.afterCreate(eventPublisher);

        // Then
        ArgumentCaptor<PaymentRecordedEvent> eventCaptor = ArgumentCaptor.forClass(PaymentRecordedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PaymentRecordedEvent event = eventCaptor.getValue();
        assertEquals(payment.getId(), event.getPaymentId());
        assertEquals(invoice.getId(), event.getInvoiceId());
        assertEquals(new BigDecimal("100.00"), event.getAmount());
    }

    // === VALIDATION TESTS ===

    @Test
    void testBeforeCreateRejectsNullInvoice() {
        // When/Then
        Payment payment = new Payment();
        assertThrows(NotFoundException.class, () -> {
            payment.beforeCreate(UUID.randomUUID(), Instant.now(), new BigDecimal("100.00"), PaymentMethod.CASH, null);
        });
    }

    @Test
    void testBeforeCreateRejectsDraftInvoice() {
        // Given
        Invoice invoice = createDraftInvoice();

        // When/Then
        Payment payment = new Payment();
        assertThrows(ValidationException.class, () -> {
            payment.beforeCreate(invoice.getId(), Instant.now(), new BigDecimal("100.00"), PaymentMethod.CASH, invoice);
        });
    }

    @Test
    void testBeforeCreateAcceptsSentInvoice() {
        // Given
        Invoice invoice = createSentInvoice();

        // When/Then - should not throw
        Payment payment = new Payment();
        assertDoesNotThrow(() -> {
            payment.beforeCreate(invoice.getId(), Instant.now(), new BigDecimal("100.00"), PaymentMethod.CASH, invoice);
        });
    }

    @Test
    void testBeforeCreateAcceptsPaidInvoice() {
        // Given
        Invoice invoice = createPaidInvoice();

        // When/Then - should not throw (overpayments allowed)
        Payment payment = new Payment();
        assertDoesNotThrow(() -> {
            payment.beforeCreate(invoice.getId(), Instant.now(), new BigDecimal("50.00"), PaymentMethod.CASH, invoice);
        });
    }

    @Test
    void testBeforeCreateAllowsPaymentDateBeforeInvoiceDate() {
        // Given: Invoice with invoice date
        Invoice invoice = createSentInvoice();
        Instant invoiceDate = invoice.getInvoiceDate();
        Instant paymentDate = invoiceDate.minus(1, ChronoUnit.DAYS);

        // When/Then: Payment date before invoice date is now allowed (Release 1.2)
        Payment payment = new Payment();
        assertDoesNotThrow(() -> {
            payment.beforeCreate(invoice.getId(), paymentDate, new BigDecimal("100.00"), PaymentMethod.CASH, invoice);
        });
    }

    @Test
    void testBeforeCreateRejectsZeroAmount() {
        // Given
        Invoice invoice = createSentInvoice();

        // When/Then
        Payment payment = new Payment();
        assertThrows(ValidationException.class, () -> {
            payment.beforeCreate(invoice.getId(), Instant.now(), BigDecimal.ZERO, PaymentMethod.CASH, invoice);
        });
    }

    @Test
    void testBeforeCreateRejectsNegativeAmount() {
        // Given
        Invoice invoice = createSentInvoice();

        // When/Then
        Payment payment = new Payment();
        assertThrows(ValidationException.class, () -> {
            payment.beforeCreate(invoice.getId(), Instant.now(), new BigDecimal("-50.00"), PaymentMethod.CASH, invoice);
        });
    }

    @Test
    void testBeforeCreateRejectsNullAmount() {
        // Given
        Invoice invoice = createSentInvoice();

        // When/Then
        Payment payment = new Payment();
        assertThrows(ValidationException.class, () -> {
            payment.beforeCreate(invoice.getId(), Instant.now(), null, PaymentMethod.CASH, invoice);
        });
    }

    @Test
    void testBeforeCreateRejectsNullPaymentMethod() {
        // Given
        Invoice invoice = createSentInvoice();

        // When/Then
        Payment payment = new Payment();
        assertThrows(ValidationException.class, () -> {
            payment.beforeCreate(invoice.getId(), Instant.now(), new BigDecimal("100.00"), null, invoice);
        });
    }

    // === DELETE TESTS ===

    @Test
    void testBeforeDeleteForSystemUpdate() {
        // Given
        Payment payment = new Payment();

        // When/Then - should not throw
        assertDoesNotThrow(() -> {
            payment.beforeDelete(true);
        });
    }

    @Test
    void testDeleteMarkAsDeleted() {
        // Given
        Invoice invoice = createSentInvoice();
        Payment payment = new Payment();
        payment.beforeCreate(invoice.getId(), Instant.now(), new BigDecimal("100.00"), PaymentMethod.CASH, invoice);
        payment.create(invoice.getId(), Instant.now(), new BigDecimal("100.00"), PaymentMethod.CASH, null, null, invoice);

        // When
        payment.delete();

        // Then
        assertTrue(payment.getIsDeleted());
        assertNotNull(payment.getDeletedAt());
        assertEquals(testUserId, payment.getDeletedBy());
    }

    @Test
    void testAfterDeleteDoesNotPublishEvent() {
        // Given
        Payment payment = new Payment();

        // When
        payment.afterDelete();

        // Then - no event should be published
        verifyNoInteractions(eventPublisher);
    }

    // === HELPER METHODS ===

    private Invoice createDraftInvoice() {
        Invoice invoice = new Invoice();
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setInvoiceNumber("INV-2025-11-09-001");
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setCustomerName("Test Customer");
        invoice.setTotal(new BigDecimal("500.00"));
        invoice.setAmountPaid(BigDecimal.ZERO);
        return invoice;
    }

    private Invoice createSentInvoice() {
        Invoice invoice = new Invoice();
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setInvoiceNumber("INV-2025-11-09-002");
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setInvoiceDate(Instant.now().minus(1, ChronoUnit.DAYS));
        invoice.setCustomerName("Test Customer");
        invoice.setTotal(new BigDecimal("500.00"));
        invoice.setAmountPaid(BigDecimal.ZERO);
        return invoice;
    }

    private Invoice createPaidInvoice() {
        Invoice invoice = new Invoice();
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setInvoiceNumber("INV-2025-11-09-003");
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setInvoiceDate(Instant.now().minus(5, ChronoUnit.DAYS));
        invoice.setCustomerName("Test Customer");
        invoice.setTotal(new BigDecimal("300.00"));
        invoice.setAmountPaid(new BigDecimal("300.00"));
        return invoice;
    }
}
