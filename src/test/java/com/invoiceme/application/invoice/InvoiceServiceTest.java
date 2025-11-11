package com.invoiceme.application.invoice;

import com.invoiceme.application.common.UserContext;
import com.invoiceme.application.invoice.dto.CreateInvoiceRequest;
import com.invoiceme.application.invoice.dto.InvoiceListItemResponse;
import com.invoiceme.application.invoice.dto.InvoiceResponse;
import com.invoiceme.application.invoice.dto.UpdateInvoiceRequest;
import com.invoiceme.domain.common.exceptions.NotFoundException;
import com.invoiceme.domain.common.exceptions.OptimisticLockException;
import com.invoiceme.domain.common.exceptions.ValidationException;
import com.invoiceme.domain.customer.Customer;
import com.invoiceme.domain.customer.events.CustomerNameChangedEvent;
import com.invoiceme.domain.invoice.Invoice;
import com.invoiceme.domain.invoice.InvoiceStatus;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
import com.invoiceme.infrastructure.persistence.InvoiceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.atLeastOnce;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private InvoiceService invoiceService;
    private InvoiceMapper invoiceMapper;

    @BeforeEach
    void setUp() {
        UserContext.setCurrentUser(UUID.randomUUID());
        invoiceMapper = new InvoiceMapper();
        invoiceService = new InvoiceService();

        // Use reflection to inject dependencies
        try {
            var invoiceRepoField = InvoiceService.class.getDeclaredField("invoiceRepository");
            invoiceRepoField.setAccessible(true);
            invoiceRepoField.set(invoiceService, invoiceRepository);

            var customerRepoField = InvoiceService.class.getDeclaredField("customerRepository");
            customerRepoField.setAccessible(true);
            customerRepoField.set(invoiceService, customerRepository);

            var eventPublisherField = InvoiceService.class.getDeclaredField("eventPublisher");
            eventPublisherField.setAccessible(true);
            eventPublisherField.set(invoiceService, eventPublisher);

            var mapperField = InvoiceService.class.getDeclaredField("invoiceMapper");
            mapperField.setAccessible(true);
            mapperField.set(invoiceService, invoiceMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // === CREATE TESTS ===

    @Test
    void testCreateInvoiceSuccess() {
        // Given
        UUID customerId = UUID.randomUUID();
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setCustomerId(customerId);
        request.setNotes("Test invoice");

        Customer customer = createTestCustomer(customerId);

        when(customerRepository.findByIdAndIsDeletedFalse(customerId))
                .thenReturn(Optional.of(customer));
        when(invoiceRepository.findInvoiceNumbersByPrefix(anyString()))
                .thenReturn(Arrays.asList());
        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(invocation -> {
                    Invoice inv = invocation.getArgument(0);
                    inv.setId(UUID.randomUUID());
                    return inv;
                });

        // When
        InvoiceResponse result = invoiceService.createInvoice(request);

        // Then
        assertNotNull(result);
        assertEquals(customerId, result.getCustomerId());
        assertEquals("Test invoice", result.getNotes());
        assertEquals(InvoiceStatus.DRAFT, result.getStatus());
        assertEquals(customer.getCompanyName(), result.getCustomerName());
        assertNotNull(result.getInvoiceNumber());

        verify(customerRepository).findByIdAndIsDeletedFalse(customerId);
        verify(invoiceRepository).save(any(Invoice.class));
        // Note: eventPublisher is called by the entity, which is tested separately in InvoiceTest
    }

    @Test
    void testCreateInvoiceWithCustomNumber() {
        // Given
        UUID customerId = UUID.randomUUID();
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setCustomerId(customerId);
        request.setInvoiceNumber("CUSTOM-001");
        request.setNotes("Custom invoice");

        Customer customer = createTestCustomer(customerId);

        when(customerRepository.findByIdAndIsDeletedFalse(customerId))
                .thenReturn(Optional.of(customer));
        when(invoiceRepository.existsByInvoiceNumberAndIsDeletedFalse("CUSTOM-001"))
                .thenReturn(false);
        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(invocation -> {
                    Invoice inv = invocation.getArgument(0);
                    inv.setId(UUID.randomUUID());
                    return inv;
                });

        // When
        InvoiceResponse result = invoiceService.createInvoice(request);

        // Then
        assertEquals("CUSTOM-001", result.getInvoiceNumber());
        verify(invoiceRepository).existsByInvoiceNumberAndIsDeletedFalse("CUSTOM-001");
    }

    @Test
    void testCreateInvoiceCustomerNotFound() {
        // Given
        UUID customerId = UUID.randomUUID();
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setCustomerId(customerId);

        when(customerRepository.findByIdAndIsDeletedFalse(customerId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () ->
                invoiceService.createInvoice(request));

        verify(customerRepository).findByIdAndIsDeletedFalse(customerId);
        verify(invoiceRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // === EVENT LISTENER TESTS ===

    @Test
    void testOnCustomerNameChanged() {
        // Given
        UUID customerId = UUID.randomUUID();
        String oldName = "Old Company";
        String newName = "New Company";

        Invoice invoice1 = createTestInvoice(customerId, "INV-001");
        Invoice invoice2 = createTestInvoice(customerId, "INV-002");

        when(invoiceRepository.findAllByCustomerIdAndIsDeletedFalse(customerId))
                .thenReturn(Arrays.asList(invoice1, invoice2));

        CustomerNameChangedEvent event = new CustomerNameChangedEvent(
                customerId, oldName, newName);

        // When
        invoiceService.onCustomerNameChanged(event);

        // Then
        assertEquals(newName, invoice1.getCustomerName());
        assertEquals(newName, invoice2.getCustomerName());
        verify(invoiceRepository, times(2)).save(any(Invoice.class));
    }

    @Test
    void testOnCustomerNameChangedWithNoInvoices() {
        // Given
        UUID customerId = UUID.randomUUID();
        CustomerNameChangedEvent event = new CustomerNameChangedEvent(
                customerId, "Old Name", "New Name");

        when(invoiceRepository.findAllByCustomerIdAndIsDeletedFalse(customerId))
                .thenReturn(Arrays.asList());

        // When
        invoiceService.onCustomerNameChanged(event);

        // Then
        verify(invoiceRepository, never()).save(any());
    }

    // === UPDATE TESTS ===

    @Test
    void testUpdateInvoiceSuccess() {
        // Given
        UUID invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Invoice invoice = createTestInvoice(customerId, "INV-001");
        invoice.setId(invoiceId);
        invoice.setVersion(0L);
        invoice.setStatus(InvoiceStatus.DRAFT);

        UpdateInvoiceRequest request = new UpdateInvoiceRequest();
        request.setId(invoiceId);
        request.setVersion(0L);
        request.setInvoiceNumber("INV-001-UPDATED");
        request.setNotes("Updated notes");

        when(invoiceRepository.findByIdAndIsDeletedFalse(invoiceId))
                .thenReturn(Optional.of(invoice));
        when(invoiceRepository.existsByInvoiceNumberAndIsDeletedFalse("INV-001-UPDATED"))
                .thenReturn(false);
        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        InvoiceResponse result = invoiceService.updateInvoice(request);

        // Then
        assertNotNull(result);
        assertEquals("INV-001-UPDATED", result.getInvoiceNumber());
        assertEquals("Updated notes", result.getNotes());
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void testUpdateInvoiceOptimisticLockException() {
        // Given
        UUID invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Invoice invoice = createTestInvoice(customerId, "INV-001");
        invoice.setId(invoiceId);
        invoice.setVersion(1L);  // Version mismatch

        UpdateInvoiceRequest request = new UpdateInvoiceRequest();
        request.setId(invoiceId);
        request.setVersion(0L);  // Old version

        when(invoiceRepository.findByIdAndIsDeletedFalse(invoiceId))
                .thenReturn(Optional.of(invoice));

        // When/Then
        assertThrows(OptimisticLockException.class, () ->
                invoiceService.updateInvoice(request));
    }

    @Test
    void testUpdateInvoiceNotDraftThrowsException() {
        // Given
        UUID invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Invoice invoice = createTestInvoice(customerId, "INV-001");
        invoice.setId(invoiceId);
        invoice.setVersion(0L);
        invoice.setStatus(InvoiceStatus.SENT);  // Not DRAFT

        UpdateInvoiceRequest request = new UpdateInvoiceRequest();
        request.setId(invoiceId);
        request.setVersion(0L);

        when(invoiceRepository.findByIdAndIsDeletedFalse(invoiceId))
                .thenReturn(Optional.of(invoice));

        // When/Then
        assertThrows(ValidationException.class, () ->
                invoiceService.updateInvoice(request));
    }

    // === GET BY ID TESTS ===

    @Test
    void testGetInvoiceByIdSuccess() {
        // Given
        UUID invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Invoice invoice = createTestInvoice(customerId, "INV-001");
        invoice.setId(invoiceId);

        when(invoiceRepository.findByIdAndIsDeletedFalse(invoiceId))
                .thenReturn(Optional.of(invoice));

        // When
        InvoiceResponse result = invoiceService.getInvoiceById(invoiceId);

        // Then
        assertNotNull(result);
        assertEquals(invoiceId, result.getId());
        assertEquals("INV-001", result.getInvoiceNumber());
        verify(invoiceRepository).findByIdAndIsDeletedFalse(invoiceId);
    }

    @Test
    void testGetInvoiceByIdNotFound() {
        // Given
        UUID invoiceId = UUID.randomUUID();

        when(invoiceRepository.findByIdAndIsDeletedFalse(invoiceId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () ->
                invoiceService.getInvoiceById(invoiceId));

        verify(invoiceRepository).findByIdAndIsDeletedFalse(invoiceId);
    }

    // === LIST TESTS ===

    @Test
    void testListAllInvoices() {
        // Given
        UUID currentUserId = UserContext.getCurrentUser();
        UUID customerId1 = UUID.randomUUID();
        UUID customerId2 = UUID.randomUUID();

        Invoice invoice1 = createTestInvoice(customerId1, "INV-001");
        Invoice invoice2 = createTestInvoice(customerId2, "INV-002");
        Invoice invoice3 = createTestInvoice(customerId1, "INV-003");

        when(invoiceRepository.findAllByCreatedByAndIsDeletedFalseOrderByInvoiceDateDesc(currentUserId))
                .thenReturn(Arrays.asList(invoice1, invoice2, invoice3));

        // When
        List<InvoiceListItemResponse> result = invoiceService.listAllInvoices();

        // Then
        assertEquals(3, result.size());
        verify(invoiceRepository).findAllByCreatedByAndIsDeletedFalseOrderByInvoiceDateDesc(currentUserId);
    }

    @Test
    void testListAllInvoicesEmpty() {
        // Given
        UUID currentUserId = UserContext.getCurrentUser();
        when(invoiceRepository.findAllByCreatedByAndIsDeletedFalseOrderByInvoiceDateDesc(currentUserId))
                .thenReturn(Arrays.asList());

        // When
        List<InvoiceListItemResponse> result = invoiceService.listAllInvoices();

        // Then
        assertEquals(0, result.size());
    }

    @Test
    void testListInvoicesByCustomer() {
        // Given
        UUID customerId = UUID.randomUUID();

        Invoice invoice1 = createTestInvoice(customerId, "INV-001");
        Invoice invoice2 = createTestInvoice(customerId, "INV-002");

        when(invoiceRepository.findAllByCustomerIdAndIsDeletedFalse(customerId))
                .thenReturn(Arrays.asList(invoice1, invoice2));

        // When
        List<InvoiceListItemResponse> result = invoiceService.listInvoicesByCustomer(customerId);

        // Then
        assertEquals(2, result.size());
        verify(invoiceRepository).findAllByCustomerIdAndIsDeletedFalse(customerId);
    }

    @Test
    void testListInvoicesByCustomerEmpty() {
        // Given
        UUID customerId = UUID.randomUUID();

        when(invoiceRepository.findAllByCustomerIdAndIsDeletedFalse(customerId))
                .thenReturn(Arrays.asList());

        // When
        List<InvoiceListItemResponse> result = invoiceService.listInvoicesByCustomer(customerId);

        // Then
        assertEquals(0, result.size());
    }

    // === SEND TESTS ===

    @Test
    void testMarkInvoiceAsSentSuccess() {
        // Given
        UUID invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Invoice invoice = createTestInvoice(customerId, "INV-001");
        invoice.setId(invoiceId);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setTotal(new BigDecimal("100.00"));  // Must have total > 0 to send

        when(invoiceRepository.findByIdAndIsDeletedFalse(invoiceId))
                .thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        InvoiceResponse result = invoiceService.markInvoiceAsSent(invoiceId);

        // Then
        assertNotNull(result);
        assertEquals(InvoiceStatus.SENT, result.getStatus());
        assertNotNull(result.getInvoiceDate());
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void testMarkInvoiceAsSentNotFound() {
        // Given
        UUID invoiceId = UUID.randomUUID();

        when(invoiceRepository.findByIdAndIsDeletedFalse(invoiceId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () ->
                invoiceService.markInvoiceAsSent(invoiceId));

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void testMarkInvoiceAsSentNotDraftThrowsException() {
        // Given
        UUID invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Invoice invoice = createTestInvoice(customerId, "INV-001");
        invoice.setId(invoiceId);
        invoice.setStatus(InvoiceStatus.SENT);  // Already SENT

        when(invoiceRepository.findByIdAndIsDeletedFalse(invoiceId))
                .thenReturn(Optional.of(invoice));

        // When/Then
        assertThrows(ValidationException.class, () ->
                invoiceService.markInvoiceAsSent(invoiceId));

        verify(invoiceRepository, never()).save(any());
    }

    // === DELETE TESTS ===

    @Test
    void testDeleteInvoiceSuccess() {
        // Given
        UUID invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Invoice invoice = createTestInvoice(customerId, "INV-001");
        invoice.setId(invoiceId);
        invoice.setStatus(InvoiceStatus.DRAFT);

        when(invoiceRepository.findByIdAndIsDeletedFalse(invoiceId))
                .thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any(Invoice.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        invoiceService.deleteInvoice(invoiceId);

        // Then
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void testDeleteInvoiceNotFound() {
        // Given
        UUID invoiceId = UUID.randomUUID();

        when(invoiceRepository.findByIdAndIsDeletedFalse(invoiceId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () ->
                invoiceService.deleteInvoice(invoiceId));

        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void testDeleteInvoiceNotDraftThrowsException() {
        // Given
        UUID invoiceId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        Invoice invoice = createTestInvoice(customerId, "INV-001");
        invoice.setId(invoiceId);
        invoice.setStatus(InvoiceStatus.SENT);  // Not DRAFT

        when(invoiceRepository.findByIdAndIsDeletedFalse(invoiceId))
                .thenReturn(Optional.of(invoice));

        // When/Then
        assertThrows(ValidationException.class, () ->
                invoiceService.deleteInvoice(invoiceId));

        verify(invoiceRepository, never()).save(any());
    }

    // === Helper Methods ===

    private Customer createTestCustomer(UUID customerId) {
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setCompanyName("Test Company");
        customer.setEmail("test@example.com");
        return customer;
    }

    private Invoice createTestInvoice(UUID customerId, String invoiceNumber) {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setCustomerId(customerId);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setCustomerName("Old Company");
        invoice.setStatus(InvoiceStatus.DRAFT);
        return invoice;
    }
}
