package com.invoiceme.application.customer;

import com.invoiceme.application.common.UserContext;
import com.invoiceme.application.customer.dto.CreateCustomerRequest;
import com.invoiceme.application.customer.dto.CustomerResponse;
import com.invoiceme.application.invoice.InvoiceService;
import com.invoiceme.application.invoice.dto.CreateInvoiceRequest;
import com.invoiceme.application.invoice.dto.InvoiceResponse;
import com.invoiceme.application.lineitem.LineItemService;
import com.invoiceme.application.lineitem.dto.CreateLineItemRequest;
import com.invoiceme.application.payment.PaymentService;
import com.invoiceme.application.payment.dto.RecordPaymentRequest;
import com.invoiceme.application.payment.dto.PaymentResponse;
import com.invoiceme.domain.common.exceptions.ValidationException;
import com.invoiceme.domain.invoice.InvoiceStatus;
import com.invoiceme.domain.payment.PaymentMethod;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for InvoiceEventHandler, focusing on payment event handling.
 * Tests the critical bug fix where partial payments should properly update customer totalOutstanding.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InvoiceEventHandlerIntegrationTest {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private LineItemService lineItemService;

    @Autowired
    private CustomerRepository customerRepository;

    private UUID testUserId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
        UserContext.setCurrentUser(testUserId);

        // Create a test customer
        CreateCustomerRequest customerRequest = new CreateCustomerRequest();
        customerRequest.setCompanyName("Test Customer Corp");
        customerRequest.setEmail("test-" + UUID.randomUUID() + "@example.com");
        CustomerResponse customer = customerService.createCustomer(customerRequest);
        customerId = customer.getId();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    /**
     * Test the critical bug fix: Partial payments should properly update customer totalOutstanding.
     * Scenario: $60 invoice → $30 payment → $30 payment
     * Expected: Customer balance is updated correctly at each step
     */
    @Test
    void testPartialPaymentsUpdateCustomerBalanceCorrectly() {
        // Create a draft invoice
        CreateInvoiceRequest invoiceRequest = new CreateInvoiceRequest();
        invoiceRequest.setCustomerId(customerId);
        invoiceRequest.setInvoiceNumber("INV-TEST-001");
        invoiceRequest.setNotes("Test invoice for partial payments");
        InvoiceResponse invoice = invoiceService.createInvoice(invoiceRequest);

        // Add line item for $60
        CreateLineItemRequest lineItemRequest = new CreateLineItemRequest();
        lineItemRequest.setInvoiceId(invoice.getId());
        lineItemRequest.setDescription("Test Service");
        lineItemRequest.setQuantity(new BigDecimal("1"));
        lineItemRequest.setUnitPrice(new BigDecimal("60.00"));
        lineItemService.createLineItem(lineItemRequest);

        // Send the invoice
        invoice = invoiceService.markInvoiceAsSent(invoice.getId());
        assertEquals(InvoiceStatus.SENT, invoice.getStatus());

        // Get customer balance after sending (should be $60 outstanding)
        CustomerResponse customerAfterSend = customerService.getCustomerById(customerId);
        assertEquals(new BigDecimal("60.00"), customerAfterSend.getTotalOutstanding(),
            "After sending $60 invoice, customer outstanding should be $60");

        // Record first payment of $30
        RecordPaymentRequest payment1 = new RecordPaymentRequest();
        payment1.setInvoiceId(invoice.getId());
        payment1.setAmount(new BigDecimal("30.00"));
        payment1.setPaymentDate(Instant.now());
        payment1.setPaymentMethod(PaymentMethod.CHECK);
        payment1.setReferenceNumber("CHK-001");

        PaymentResponse paymentResponse1 = paymentService.recordPayment(payment1);
        assertNotNull(paymentResponse1);
        assertEquals(new BigDecimal("30.00"), paymentResponse1.getAmount());

        // Verify customer totalOutstanding decreased to $30
        CustomerResponse customerAfterPayment1 = customerService.getCustomerById(customerId);
        assertEquals(new BigDecimal("30.00"), customerAfterPayment1.getTotalOutstanding(),
            "After first $30 payment, customer outstanding should be $30");

        // Record second payment of $30
        RecordPaymentRequest payment2 = new RecordPaymentRequest();
        payment2.setInvoiceId(invoice.getId());
        payment2.setAmount(new BigDecimal("30.00"));
        payment2.setPaymentDate(Instant.now());
        payment2.setPaymentMethod(PaymentMethod.CHECK);
        payment2.setReferenceNumber("CHK-002");

        PaymentResponse paymentResponse2 = paymentService.recordPayment(payment2);
        assertNotNull(paymentResponse2);
        assertEquals(new BigDecimal("30.00"), paymentResponse2.getAmount());

        // Verify customer totalOutstanding is now $0
        CustomerResponse customerAfterPayment2 = customerService.getCustomerById(customerId);
        assertEquals(0, customerAfterPayment2.getTotalOutstanding().compareTo(BigDecimal.ZERO),
            "After second $30 payment, customer outstanding should be $0");
    }

    /**
     * Test that multiple payments to different invoices correctly update customer balance.
     */
    @Test
    void testMultipleInvoicesWithPayments() {
        // Create first invoice with $50 line item
        CreateInvoiceRequest invoice1Request = new CreateInvoiceRequest();
        invoice1Request.setCustomerId(customerId);
        invoice1Request.setInvoiceNumber("INV-TEST-002");
        InvoiceResponse invoice1 = invoiceService.createInvoice(invoice1Request);

        CreateLineItemRequest lineItem1 = new CreateLineItemRequest();
        lineItem1.setInvoiceId(invoice1.getId());
        lineItem1.setDescription("Service 1");
        lineItem1.setQuantity(new BigDecimal("1"));
        lineItem1.setUnitPrice(new BigDecimal("50.00"));
        lineItemService.createLineItem(lineItem1);

        invoice1 = invoiceService.markInvoiceAsSent(invoice1.getId());

        // Create second invoice with $25 line item
        CreateInvoiceRequest invoice2Request = new CreateInvoiceRequest();
        invoice2Request.setCustomerId(customerId);
        invoice2Request.setInvoiceNumber("INV-TEST-003");
        InvoiceResponse invoice2 = invoiceService.createInvoice(invoice2Request);

        CreateLineItemRequest lineItem2 = new CreateLineItemRequest();
        lineItem2.setInvoiceId(invoice2.getId());
        lineItem2.setDescription("Service 2");
        lineItem2.setQuantity(new BigDecimal("1"));
        lineItem2.setUnitPrice(new BigDecimal("25.00"));
        lineItemService.createLineItem(lineItem2);

        invoice2 = invoiceService.markInvoiceAsSent(invoice2.getId());

        // Total outstanding should be $75
        CustomerResponse customerAfterSend = customerService.getCustomerById(customerId);
        assertEquals(new BigDecimal("75.00"), customerAfterSend.getTotalOutstanding(),
            "After sending both invoices, outstanding should be $75");

        // Pay $50 on first invoice
        RecordPaymentRequest payment1 = new RecordPaymentRequest();
        payment1.setInvoiceId(invoice1.getId());
        payment1.setAmount(new BigDecimal("50.00"));
        payment1.setPaymentDate(Instant.now());
        payment1.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        paymentService.recordPayment(payment1);

        // Verify customer balance is now $25
        CustomerResponse customerAfterPayment1 = customerService.getCustomerById(customerId);
        assertEquals(new BigDecimal("25.00"), customerAfterPayment1.getTotalOutstanding(),
            "After $50 payment to first invoice, outstanding should be $25");

        // Pay $25 on second invoice
        RecordPaymentRequest payment2 = new RecordPaymentRequest();
        payment2.setInvoiceId(invoice2.getId());
        payment2.setAmount(new BigDecimal("25.00"));
        payment2.setPaymentDate(Instant.now());
        payment2.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
        paymentService.recordPayment(payment2);

        // Verify customer balance is now $0
        CustomerResponse customerAfterPayment2 = customerService.getCustomerById(customerId);
        assertEquals(0, customerAfterPayment2.getTotalOutstanding().compareTo(BigDecimal.ZERO),
            "After $25 payment to second invoice, total outstanding should be $0");
    }

    /**
     * Test that payments cannot be recorded for DRAFT invoices.
     */
    @Test
    void testPaymentOnDraftInvoiceIsBlocked() {
        // Create draft invoice
        CreateInvoiceRequest invoiceRequest = new CreateInvoiceRequest();
        invoiceRequest.setCustomerId(customerId);
        invoiceRequest.setInvoiceNumber("INV-TEST-004");
        InvoiceResponse invoice = invoiceService.createInvoice(invoiceRequest);

        // Attempt to record payment on draft invoice
        RecordPaymentRequest payment = new RecordPaymentRequest();
        payment.setInvoiceId(invoice.getId());
        payment.setAmount(new BigDecimal("100.00"));
        payment.setPaymentDate(Instant.now());
        payment.setPaymentMethod(PaymentMethod.CHECK);

        // Should throw ValidationException
        ValidationException exception = assertThrows(ValidationException.class, () ->
            paymentService.recordPayment(payment));

        assertTrue(exception.getMessage().contains("SENT or PAID"),
            "Exception message should indicate payments are only for SENT or PAID invoices");
    }
}
