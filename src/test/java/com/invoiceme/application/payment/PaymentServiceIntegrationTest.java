package com.invoiceme.application.payment;

import com.invoiceme.application.common.UserContext;
import com.invoiceme.application.customer.CustomerService;
import com.invoiceme.application.customer.dto.CreateCustomerRequest;
import com.invoiceme.application.customer.dto.CustomerResponse;
import com.invoiceme.application.invoice.InvoiceService;
import com.invoiceme.application.invoice.dto.CreateInvoiceRequest;
import com.invoiceme.application.invoice.dto.InvoiceResponse;
import com.invoiceme.application.payment.dto.PaymentResponse;
import com.invoiceme.application.payment.dto.RecordPaymentRequest;
import com.invoiceme.domain.common.exceptions.NotFoundException;
import com.invoiceme.domain.common.exceptions.ValidationException;
import com.invoiceme.domain.invoice.InvoiceStatus;
import com.invoiceme.domain.payment.PaymentMethod;
import com.invoiceme.infrastructure.persistence.InvoiceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private InvoiceRepository invoiceRepository;

    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        UserContext.setCurrentUser(testUserId);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // === RECORD PAYMENT TESTS ===

    @Test
    void testRecordPaymentSuccessfully() {
        // Given: Create customer and SENT invoice
        CustomerResponse customer = createTestCustomer("Test Customer");
        InvoiceResponse invoice = createTestInvoice(customer.getId());
        invoiceService.markInvoiceAsSent(invoice.getId());
        invoice = invoiceService.getInvoiceById(invoice.getId()); // Refresh

        // When: Record payment
        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setInvoiceId(invoice.getId());
        request.setPaymentDate(Instant.now());
        request.setAmount(new BigDecimal("150.00"));
        request.setPaymentMethod(PaymentMethod.CASH);
        request.setReferenceNumber("REF-12345");
        request.setNotes("Test payment");

        PaymentResponse payment = paymentService.recordPayment(request);

        // Then
        assertNotNull(payment);
        assertNotNull(payment.getId());
        assertEquals(invoice.getId(), payment.getInvoiceId());
        assertEquals(new BigDecimal("150.00"), payment.getAmount());
        assertEquals(PaymentMethod.CASH, payment.getPaymentMethod());
        assertEquals("REF-12345", payment.getReferenceNumber());
        assertEquals("Test payment", payment.getNotes());
        assertEquals(customer.getCompanyName(), payment.getCustomerName());
    }

    @Test
    void testRecordPaymentForDraftInvoiceThrowsException() {
        // Given: Create DRAFT invoice
        CustomerResponse customer = createTestCustomer("Test Customer");
        InvoiceResponse invoice = createTestInvoice(customer.getId());

        // When/Then: Try to record payment for DRAFT invoice
        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setInvoiceId(invoice.getId());
        request.setPaymentDate(Instant.now());
        request.setAmount(new BigDecimal("100.00"));
        request.setPaymentMethod(PaymentMethod.CASH);

        assertThrows(ValidationException.class, () ->
            paymentService.recordPayment(request));
    }

    @Test
    void testRecordPaymentForNonExistentInvoiceThrowsException() {
        // When/Then
        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setInvoiceId(UUID.randomUUID());
        request.setPaymentDate(Instant.now());
        request.setAmount(new BigDecimal("100.00"));
        request.setPaymentMethod(PaymentMethod.CASH);

        assertThrows(NotFoundException.class, () ->
            paymentService.recordPayment(request));
    }

    @Test
    void testRecordFullPaymentTransitionsInvoiceToPaid() {
        // Given: Create customer and SENT invoice with total $500
        CustomerResponse customer = createTestCustomer("Test Customer");
        InvoiceResponse invoice = createTestInvoice(customer.getId());
        invoice = invoiceService.markInvoiceAsSent(invoice.getId());

        // When: Record payment for full amount
        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setInvoiceId(invoice.getId());
        request.setPaymentDate(Instant.now());
        request.setAmount(new BigDecimal("500.00"));
        request.setPaymentMethod(PaymentMethod.CREDIT_CARD);

        paymentService.recordPayment(request);

        // Then: Invoice should transition to PAID
        invoice = invoiceService.getInvoiceById(invoice.getId());
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(new BigDecimal("500.00"), invoice.getAmountPaid());
        assertEquals(0, invoice.getBalance().compareTo(BigDecimal.ZERO));

        // And: Customer statistics should be updated
        customer = customerService.getCustomerById(customer.getId());
        assertEquals(0, customer.getSentInvoiceCount());
        assertEquals(1, customer.getPaidInvoiceCount());
        assertEquals(0, customer.getTotalOutstanding().compareTo(BigDecimal.ZERO));
    }

    @Test
    void testRecordPartialPaymentKeepsInvoiceSent() {
        // Given: Create customer and SENT invoice with total $500
        CustomerResponse customer = createTestCustomer("Test Customer");
        InvoiceResponse invoice = createTestInvoice(customer.getId());
        invoice = invoiceService.markInvoiceAsSent(invoice.getId());

        // When: Record partial payment
        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setInvoiceId(invoice.getId());
        request.setPaymentDate(Instant.now());
        request.setAmount(new BigDecimal("200.00"));
        request.setPaymentMethod(PaymentMethod.CHECK);
        request.setReferenceNumber("CHK-001");

        paymentService.recordPayment(request);

        // Then: Invoice should remain SENT
        invoice = invoiceService.getInvoiceById(invoice.getId());
        assertEquals(InvoiceStatus.SENT, invoice.getStatus());
        assertEquals(new BigDecimal("200.00"), invoice.getAmountPaid());
        assertEquals(new BigDecimal("300.00"), invoice.getBalance());

        // And: Customer statistics should NOT change (still SENT)
        customer = customerService.getCustomerById(customer.getId());
        assertEquals(1, customer.getSentInvoiceCount());
        assertEquals(0, customer.getPaidInvoiceCount());
        assertEquals(new BigDecimal("500.00"), customer.getTotalOutstanding());
    }

    @Test
    void testRecordMultiplePartialPayments() {
        // Given: Create customer and SENT invoice with total $500
        CustomerResponse customer = createTestCustomer("Test Customer");
        InvoiceResponse invoice = createTestInvoice(customer.getId());
        invoice = invoiceService.markInvoiceAsSent(invoice.getId());

        // When: Record first partial payment
        RecordPaymentRequest payment1 = new RecordPaymentRequest();
        payment1.setInvoiceId(invoice.getId());
        payment1.setPaymentDate(Instant.now());
        payment1.setAmount(new BigDecimal("200.00"));
        payment1.setPaymentMethod(PaymentMethod.CASH);
        paymentService.recordPayment(payment1);

        // Then: Invoice should remain SENT
        invoice = invoiceService.getInvoiceById(invoice.getId());
        assertEquals(InvoiceStatus.SENT, invoice.getStatus());
        assertEquals(new BigDecimal("200.00"), invoice.getAmountPaid());

        // When: Record second partial payment
        RecordPaymentRequest payment2 = new RecordPaymentRequest();
        payment2.setInvoiceId(invoice.getId());
        payment2.setPaymentDate(Instant.now());
        payment2.setAmount(new BigDecimal("300.00"));
        payment2.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        paymentService.recordPayment(payment2);

        // Then: Invoice should transition to PAID
        invoice = invoiceService.getInvoiceById(invoice.getId());
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(new BigDecimal("500.00"), invoice.getAmountPaid());
        assertEquals(0, invoice.getBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void testRecordOverpaymentTransitionsInvoiceToPaid() {
        // Given: Create customer and SENT invoice with total $500
        CustomerResponse customer = createTestCustomer("Test Customer");
        InvoiceResponse invoice = createTestInvoice(customer.getId());
        invoice = invoiceService.markInvoiceAsSent(invoice.getId());

        // When: Record payment exceeding total (overpayment)
        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setInvoiceId(invoice.getId());
        request.setPaymentDate(Instant.now());
        request.setAmount(new BigDecimal("750.00"));
        request.setPaymentMethod(PaymentMethod.BANK_TRANSFER);

        paymentService.recordPayment(request);

        // Then: Invoice should transition to PAID (overpayments allowed)
        invoice = invoiceService.getInvoiceById(invoice.getId());
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals(new BigDecimal("750.00"), invoice.getAmountPaid());
        assertTrue(invoice.getBalance().compareTo(BigDecimal.ZERO) < 0); // Negative balance
    }

    @Test
    void testRecordPaymentForAlreadyPaidInvoice() {
        // Given: Create customer and invoice, mark as SENT, then record full payment
        CustomerResponse customer = createTestCustomer("Test Customer");
        InvoiceResponse invoice = createTestInvoice(customer.getId());
        invoice = invoiceService.markInvoiceAsSent(invoice.getId());

        RecordPaymentRequest payment1 = new RecordPaymentRequest();
        payment1.setInvoiceId(invoice.getId());
        payment1.setPaymentDate(Instant.now());
        payment1.setAmount(new BigDecimal("500.00"));
        payment1.setPaymentMethod(PaymentMethod.CASH);
        paymentService.recordPayment(payment1);

        // When: Record additional payment for PAID invoice
        RecordPaymentRequest payment2 = new RecordPaymentRequest();
        payment2.setInvoiceId(invoice.getId());
        payment2.setPaymentDate(Instant.now());
        payment2.setAmount(new BigDecimal("50.00"));
        payment2.setPaymentMethod(PaymentMethod.CASH);

        // Then: Should succeed (overpayments allowed)
        assertDoesNotThrow(() -> paymentService.recordPayment(payment2));

        invoice = invoiceService.getInvoiceById(invoice.getId());
        assertEquals(new BigDecimal("550.00"), invoice.getAmountPaid());
    }

    // === LIST PAYMENTS TESTS ===

    @Test
    void testListPaymentsForInvoice() {
        // Given: Create customer, invoice, and record multiple payments
        CustomerResponse customer = createTestCustomer("Test Customer");
        InvoiceResponse invoice = createTestInvoice(customer.getId());
        invoice = invoiceService.markInvoiceAsSent(invoice.getId());

        RecordPaymentRequest payment1 = new RecordPaymentRequest();
        payment1.setInvoiceId(invoice.getId());
        payment1.setPaymentDate(Instant.now());
        payment1.setAmount(new BigDecimal("200.00"));
        payment1.setPaymentMethod(PaymentMethod.CASH);
        paymentService.recordPayment(payment1);

        RecordPaymentRequest payment2 = new RecordPaymentRequest();
        payment2.setInvoiceId(invoice.getId());
        payment2.setPaymentDate(Instant.now());
        payment2.setAmount(new BigDecimal("150.00"));
        payment2.setPaymentMethod(PaymentMethod.CHECK);
        paymentService.recordPayment(payment2);

        // When: List payments for invoice
        List<PaymentResponse> payments = paymentService.listPaymentsForInvoice(invoice.getId());

        // Then
        assertEquals(2, payments.size());
        assertEquals(new BigDecimal("200.00"), payments.get(0).getAmount());
        assertEquals(new BigDecimal("150.00"), payments.get(1).getAmount());
    }

    @Test
    void testListPaymentsForInvoiceReturnsEmptyListWhenNoPayments() {
        // Given: Create customer and invoice without payments
        CustomerResponse customer = createTestCustomer("Test Customer");
        InvoiceResponse invoice = createTestInvoice(customer.getId());

        // When: List payments
        List<PaymentResponse> payments = paymentService.listPaymentsForInvoice(invoice.getId());

        // Then
        assertEquals(0, payments.size());
    }

    // === GET PAYMENT BY ID TESTS ===

    @Test
    void testGetPaymentById() {
        // Given: Create payment
        CustomerResponse customer = createTestCustomer("Test Customer");
        InvoiceResponse invoice = createTestInvoice(customer.getId());
        invoice = invoiceService.markInvoiceAsSent(invoice.getId());

        RecordPaymentRequest request = new RecordPaymentRequest();
        request.setInvoiceId(invoice.getId());
        request.setPaymentDate(Instant.now());
        request.setAmount(new BigDecimal("100.00"));
        request.setPaymentMethod(PaymentMethod.CASH);
        PaymentResponse created = paymentService.recordPayment(request);

        // When: Get payment by ID
        PaymentResponse retrieved = paymentService.getPaymentById(created.getId());

        // Then
        assertEquals(created.getId(), retrieved.getId());
        assertEquals(created.getAmount(), retrieved.getAmount());
        assertEquals(created.getPaymentMethod(), retrieved.getPaymentMethod());
    }

    @Test
    void testGetPaymentByIdThrowsNotFoundForNonExistentPayment() {
        // When/Then
        assertThrows(NotFoundException.class, () ->
            paymentService.getPaymentById(UUID.randomUUID()));
    }

    // === HELPER METHODS ===

    private CustomerResponse createTestCustomer(String companyName) {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName(companyName);
        request.setEmail(companyName.toLowerCase().replace(" ", "") + "@test.com");
        return customerService.createCustomer(request);
    }

    private InvoiceResponse createTestInvoice(UUID customerId) {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setCustomerId(customerId);
        request.setNotes("Test invoice");
        InvoiceResponse invoice = invoiceService.createInvoice(request);

        // Manually set total for testing (since line items aren't implemented yet)
        // In real application, total would be calculated from line items
        com.invoiceme.domain.invoice.Invoice invoiceEntity = invoiceRepository.findByIdAndIsDeletedFalse(invoice.getId()).orElseThrow();
        invoiceEntity.setTotal(new BigDecimal("500.00"));
        invoiceRepository.save(invoiceEntity);

        return invoiceService.getInvoiceById(invoice.getId());
    }
}
