package com.invoiceme.application.lineitem;

import com.invoiceme.application.common.UserContext;
import com.invoiceme.application.customer.CustomerService;
import com.invoiceme.application.customer.dto.CreateCustomerRequest;
import com.invoiceme.application.customer.dto.CustomerResponse;
import com.invoiceme.application.invoice.InvoiceService;
import com.invoiceme.application.invoice.dto.CreateInvoiceRequest;
import com.invoiceme.application.invoice.dto.InvoiceResponse;
import com.invoiceme.application.lineitem.dto.CreateLineItemRequest;
import com.invoiceme.application.lineitem.dto.LineItemResponse;
import com.invoiceme.application.lineitem.dto.UpdateLineItemRequest;
import com.invoiceme.domain.common.exceptions.NotFoundException;
import com.invoiceme.domain.common.exceptions.ValidationException;
import com.invoiceme.domain.invoice.InvoiceStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for LineItem functionality.
 * Tests the complete flow including event-driven invoice total recalculation.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LineItemIntegrationTest {

    @Autowired
    private LineItemService lineItemService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private CustomerService customerService;

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

    // === CREATE TESTS ===

    @Test
    void testCreateLineItem_Success() {
        // Given: Customer and Invoice
        CustomerResponse customer = createTestCustomer();
        InvoiceResponse invoice = createTestInvoice(customer.getId());

        // When: Create line item
        CreateLineItemRequest request = new CreateLineItemRequest();
        request.setInvoiceId(invoice.getId());
        request.setDescription("Consulting services");
        request.setQuantity(new BigDecimal("5.00"));
        request.setUnitPrice(new BigDecimal("100.00"));

        LineItemResponse lineItem = lineItemService.createLineItem(request);

        // Then: Line item created with correct line total
        assertNotNull(lineItem.getId());
        assertEquals("Consulting services", lineItem.getDescription());
        assertEquals(new BigDecimal("5.00"), lineItem.getQuantity());
        assertEquals(new BigDecimal("100.00"), lineItem.getUnitPrice());
        assertEquals(new BigDecimal("500.00"), lineItem.getLineTotal());
        assertEquals(customer.getCompanyName(), lineItem.getCustomerName());

        // And: Invoice total updated
        InvoiceResponse updatedInvoice = invoiceService.getInvoiceById(invoice.getId());
        assertEquals(new BigDecimal("500.00"), updatedInvoice.getTotal());
    }

    @Test
    void testCreateLineItem_AutoCalculatesLineTotal() {
        // Given: Customer and Invoice
        CustomerResponse customer = createTestCustomer();
        InvoiceResponse invoice = createTestInvoice(customer.getId());

        // When: Create line item with quantity 2.5 and unit price 10.50
        CreateLineItemRequest request = new CreateLineItemRequest();
        request.setInvoiceId(invoice.getId());
        request.setDescription("Widget");
        request.setQuantity(new BigDecimal("2.5"));
        request.setUnitPrice(new BigDecimal("10.50"));

        LineItemResponse lineItem = lineItemService.createLineItem(request);

        // Then: Line total = 2.5 × 10.50 = 26.25
        assertEquals(new BigDecimal("26.25"), lineItem.getLineTotal());
    }

    @Test
    void testCreateLineItem_InvoiceNotDraft() {
        // Given: Invoice marked as SENT
        CustomerResponse customer = createTestCustomer();
        InvoiceResponse invoice = createTestInvoice(customer.getId());

        // Add a line item first so the invoice has a non-zero total
        CreateLineItemRequest lineItemRequest = new CreateLineItemRequest();
        lineItemRequest.setInvoiceId(invoice.getId());
        lineItemRequest.setDescription("Initial item");
        lineItemRequest.setQuantity(new BigDecimal("1.00"));
        lineItemRequest.setUnitPrice(new BigDecimal("100.00"));
        lineItemService.createLineItem(lineItemRequest);

        // Mark invoice as SENT
        invoiceService.markInvoiceAsSent(invoice.getId());

        // When: Try to create line item
        CreateLineItemRequest request = new CreateLineItemRequest();
        request.setInvoiceId(invoice.getId());
        request.setDescription("Another item");
        request.setQuantity(new BigDecimal("1.00"));
        request.setUnitPrice(new BigDecimal("50.00"));

        // Then: Validation exception thrown
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            lineItemService.createLineItem(request);
        });
        assertTrue(exception.getMessage().contains("DRAFT"));
    }

    @Test
    void testCreateLineItem_NegativeQuantity() {
        // Given: Customer and Invoice
        CustomerResponse customer = createTestCustomer();
        InvoiceResponse invoice = createTestInvoice(customer.getId());

        // When: Create line item with negative quantity
        CreateLineItemRequest request = new CreateLineItemRequest();
        request.setInvoiceId(invoice.getId());
        request.setDescription("Invalid item");
        request.setQuantity(new BigDecimal("-5.00"));
        request.setUnitPrice(new BigDecimal("100.00"));

        // Then: Validation exception thrown
        assertThrows(ValidationException.class, () -> {
            lineItemService.createLineItem(request);
        });
    }

    // === LIST TESTS ===

    @Test
    void testListLineItemsForInvoice() {
        // Given: Customer, Invoice, and multiple line items
        CustomerResponse customer = createTestCustomer();
        InvoiceResponse invoice = createTestInvoice(customer.getId());

        createLineItem(invoice.getId(), "Item 1", "2.00", "50.00");
        createLineItem(invoice.getId(), "Item 2", "1.00", "100.00");

        // When: List line items
        List<LineItemResponse> lineItems = lineItemService.listLineItemsForInvoice(invoice.getId());

        // Then: Both line items returned in order
        assertEquals(2, lineItems.size());
        assertEquals("Item 1", lineItems.get(0).getDescription());
        assertEquals("Item 2", lineItems.get(1).getDescription());
    }

    // === UPDATE TESTS ===

    @Test
    void testUpdateLineItem_Success() {
        // Given: Customer, Invoice, and Line Item
        CustomerResponse customer = createTestCustomer();
        InvoiceResponse invoice = createTestInvoice(customer.getId());
        LineItemResponse lineItem = createLineItem(invoice.getId(), "Original", "2.00", "50.00");

        // When: Update line item
        UpdateLineItemRequest request = new UpdateLineItemRequest();
        request.setId(lineItem.getId());
        request.setVersion(lineItem.getVersion());
        request.setDescription("Updated");
        request.setQuantity(new BigDecimal("3.00"));
        request.setUnitPrice(new BigDecimal("75.00"));

        LineItemResponse updated = lineItemService.updateLineItem(request);

        // Then: Line item updated with new line total
        assertEquals("Updated", updated.getDescription());
        assertEquals(new BigDecimal("3.00"), updated.getQuantity());
        assertEquals(new BigDecimal("75.00"), updated.getUnitPrice());
        assertEquals(new BigDecimal("225.00"), updated.getLineTotal());

        // And: Invoice total updated
        InvoiceResponse updatedInvoice = invoiceService.getInvoiceById(invoice.getId());
        assertEquals(new BigDecimal("225.00"), updatedInvoice.getTotal());
    }

    // === DELETE TESTS ===

    @Test
    void testDeleteLineItem_Success() {
        // Given: Customer, Invoice, and two Line Items
        CustomerResponse customer = createTestCustomer();
        InvoiceResponse invoice = createTestInvoice(customer.getId());
        LineItemResponse lineItem1 = createLineItem(invoice.getId(), "Item 1", "2.00", "50.00");
        LineItemResponse lineItem2 = createLineItem(invoice.getId(), "Item 2", "1.00", "100.00");

        // Invoice total should be 200.00 (100 + 100)
        InvoiceResponse invoiceBeforeDelete = invoiceService.getInvoiceById(invoice.getId());
        assertEquals(new BigDecimal("200.00"), invoiceBeforeDelete.getTotal());

        // When: Delete first line item
        lineItemService.deleteLineItem(lineItem1.getId(), lineItem1.getVersion());

        // Then: Line item deleted
        assertThrows(NotFoundException.class, () -> {
            lineItemService.listLineItemsForInvoice(invoice.getId()).stream()
                    .filter(li -> li.getId().equals(lineItem1.getId()))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Line item not found"));
        });

        // And: Invoice total updated to only include remaining line item
        InvoiceResponse updatedInvoice = invoiceService.getInvoiceById(invoice.getId());
        assertEquals(new BigDecimal("100.00"), updatedInvoice.getTotal());
    }

    @Test
    void testDeleteLineItem_InvoiceNotDraft() {
        // Given: Invoice marked as SENT
        CustomerResponse customer = createTestCustomer();
        InvoiceResponse invoice = createTestInvoice(customer.getId());
        LineItemResponse lineItem = createLineItem(invoice.getId(), "Item", "1.00", "100.00");

        invoiceService.markInvoiceAsSent(invoice.getId());

        // When: Try to delete line item
        // Then: Validation exception thrown
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            lineItemService.deleteLineItem(lineItem.getId(), lineItem.getVersion());
        });
        assertTrue(exception.getMessage().contains("DRAFT"));
    }

    // === EVENT LISTENER TESTS ===

    @Test
    void testMultipleLineItems_InvoiceTotalRecalculation() {
        // Given: Customer and Invoice
        CustomerResponse customer = createTestCustomer();
        InvoiceResponse invoice = createTestInvoice(customer.getId());

        // When: Add multiple line items
        createLineItem(invoice.getId(), "Item 1", "2.00", "50.00");   // 100.00
        createLineItem(invoice.getId(), "Item 2", "3.00", "25.00");   // 75.00
        createLineItem(invoice.getId(), "Item 3", "1.50", "40.00");   // 60.00

        // Then: Invoice total is sum of all line totals
        InvoiceResponse updatedInvoice = invoiceService.getInvoiceById(invoice.getId());
        assertEquals(new BigDecimal("235.00"), updatedInvoice.getTotal());
    }

    // === HELPER METHODS ===

    private CustomerResponse createTestCustomer() {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("Test Company " + UUID.randomUUID());
        request.setEmail("test" + UUID.randomUUID() + "@example.com");
        request.setContactFirstName("John");
        request.setContactLastName("Doe");
        return customerService.createCustomer(request);
    }

    private InvoiceResponse createTestInvoice(UUID customerId) {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setCustomerId(customerId);
        request.setNotes("Test invoice");
        return invoiceService.createInvoice(request);
    }

    private LineItemResponse createLineItem(UUID invoiceId, String description, String quantity, String unitPrice) {
        CreateLineItemRequest request = new CreateLineItemRequest();
        request.setInvoiceId(invoiceId);
        request.setDescription(description);
        request.setQuantity(new BigDecimal(quantity));
        request.setUnitPrice(new BigDecimal(unitPrice));
        return lineItemService.createLineItem(request);
    }
}
