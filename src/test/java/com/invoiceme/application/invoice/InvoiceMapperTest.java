package com.invoiceme.application.invoice;

import com.invoiceme.application.invoice.dto.InvoiceListItemResponse;
import com.invoiceme.application.invoice.dto.InvoiceResponse;
import com.invoiceme.domain.invoice.Invoice;
import com.invoiceme.domain.invoice.InvoiceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceMapperTest {

    private InvoiceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new InvoiceMapper();
    }

    @Test
    void testToResponseMapsAllFields() {
        // Given
        Invoice invoice = createTestInvoice();

        // When
        InvoiceResponse response = mapper.toResponse(invoice);

        // Then - Business fields
        assertEquals(invoice.getCustomerId(), response.getCustomerId());
        assertEquals("INV-2025-11-09-001", response.getInvoiceNumber());
        assertEquals("Test notes", response.getNotes());
        assertEquals(InvoiceStatus.DRAFT, response.getStatus());
        assertEquals("Acme Corp", response.getCustomerName());
        assertEquals(new BigDecimal("100.00"), response.getTotal());
        assertEquals(new BigDecimal("30.00"), response.getAmountPaid());

        // Then - Calculated field
        assertEquals(new BigDecimal("70.00"), response.getBalance());

        // Then - BaseEntity fields
        assertEquals(invoice.getId(), response.getId());
        assertEquals(invoice.getCreatedAt(), response.getCreatedAt());
        assertEquals(invoice.getCreatedBy(), response.getCreatedBy());
        assertEquals(invoice.getLastModifiedAt(), response.getLastModifiedAt());
        assertEquals(invoice.getLastModifiedBy(), response.getLastModifiedBy());
        assertEquals(invoice.getVersion(), response.getVersion());
    }

    @Test
    void testToResponseCalculatesBalanceCorrectly() {
        // Given
        Invoice invoice = new Invoice();
        invoice.setTotal(new BigDecimal("500.00"));
        invoice.setAmountPaid(new BigDecimal("150.00"));

        // When
        InvoiceResponse response = mapper.toResponse(invoice);

        // Then
        assertEquals(new BigDecimal("350.00"), response.getBalance());
    }

    @Test
    void testToResponseHandlesNullInvoice() {
        // When
        InvoiceResponse response = mapper.toResponse(null);

        // Then
        assertNull(response);
    }

    @Test
    void testToListItemMapsEssentialFields() {
        // Given
        Invoice invoice = createTestInvoice();

        // When
        InvoiceListItemResponse response = mapper.toListItem(invoice);

        // Then
        assertEquals(invoice.getId(), response.getId());
        assertEquals("INV-2025-11-09-001", response.getInvoiceNumber());
        assertEquals(InvoiceStatus.DRAFT, response.getStatus());
        assertEquals("Acme Corp", response.getCustomerName());
        assertEquals(new BigDecimal("100.00"), response.getTotal());
        assertEquals(new BigDecimal("30.00"), response.getAmountPaid());
        assertEquals(new BigDecimal("70.00"), response.getBalance());
    }

    @Test
    void testToListItemCalculatesBalanceCorrectly() {
        // Given
        Invoice invoice = new Invoice();
        invoice.setTotal(new BigDecimal("1000.00"));
        invoice.setAmountPaid(new BigDecimal("250.00"));

        // When
        InvoiceListItemResponse response = mapper.toListItem(invoice);

        // Then
        assertEquals(new BigDecimal("750.00"), response.getBalance());
    }

    @Test
    void testToListItemHandlesNullInvoice() {
        // When
        InvoiceListItemResponse response = mapper.toListItem(null);

        // Then
        assertNull(response);
    }

    @Test
    void testToListItemHandlesZeroBalance() {
        // Given
        Invoice invoice = new Invoice();
        invoice.setTotal(new BigDecimal("100.00"));
        invoice.setAmountPaid(new BigDecimal("100.00"));

        // When
        InvoiceListItemResponse response = mapper.toListItem(invoice);

        // Then
        assertEquals(0, response.getBalance().compareTo(BigDecimal.ZERO));
    }

    // Helper method to create test invoice
    private Invoice createTestInvoice() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setCustomerId(UUID.randomUUID());
        invoice.setInvoiceNumber("INV-2025-11-09-001");
        invoice.setNotes("Test notes");
        invoice.setInvoiceDate(Instant.now());
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setCustomerName("Acme Corp");
        invoice.setTotal(new BigDecimal("100.00"));
        invoice.setAmountPaid(new BigDecimal("30.00"));
        invoice.setVersion(0L);

        return invoice;
    }
}
