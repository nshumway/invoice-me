package com.invoiceme.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceme.application.common.UserContext;
import com.invoiceme.application.customer.dto.CreateCustomerRequest;
import com.invoiceme.application.customer.dto.CustomerResponse;
import com.invoiceme.application.invoice.dto.CreateInvoiceRequest;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
import com.invoiceme.infrastructure.persistence.InvoiceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    private UUID testUserId;
    private UUID testCustomerId;

    // Request post processor to set UserContext for each request
    private RequestPostProcessor setUserContext() {
        return request -> {
            UserContext.setCurrentUser(testUserId);
            return request;
        };
    }

    @BeforeEach
    void setUp() throws Exception {
        testUserId = UUID.randomUUID();

        // Create a test customer first
        CreateCustomerRequest customerRequest = new CreateCustomerRequest();
        customerRequest.setCompanyName("Test Invoice Customer");
        customerRequest.setEmail("invoice-test@example.com");

        MvcResult result = mockMvc.perform(post("/api/customers")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        // Parse the response to get customer ID
        // Response structure: { "success": true, "data": { "id": "..." } }
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(responseBody);
        testCustomerId = UUID.fromString(jsonNode.get("data").get("id").asText());
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // === CREATE TESTS ===

    @Test
    void testCreateInvoiceSuccess() throws Exception {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setCustomerId(testCustomerId);
        request.setNotes("Test invoice for API");

        mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Invoice created successfully"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.customerId").value(testCustomerId.toString()))
                .andExpect(jsonPath("$.data.invoiceNumber").exists())
                .andExpect(jsonPath("$.data.invoiceNumber").value(matchesPattern("INV-\\d{4}-\\d{2}-\\d{2}-\\d{3}")))
                .andExpect(jsonPath("$.data.notes").value("Test invoice for API"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.customerName").value("Test Invoice Customer"))
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.amountPaid").value(0))
                .andExpect(jsonPath("$.data.balance").value(0))
                .andExpect(jsonPath("$.data.version").value(0))
                .andExpect(jsonPath("$.data.invoiceDate").value(nullValue()));
    }

    @Test
    void testCreateInvoiceWithCustomNumber() throws Exception {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setCustomerId(testCustomerId);
        request.setInvoiceNumber("CUSTOM-TEST-001");
        request.setNotes("Custom numbered invoice");

        mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.invoiceNumber").value("CUSTOM-TEST-001"))
                .andExpect(jsonPath("$.data.notes").value("Custom numbered invoice"));
    }

    @Test
    void testCreateInvoiceValidationErrorMissingCustomer() throws Exception {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        // Missing customerId
        request.setNotes("Invoice without customer");

        mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testCreateInvoiceCustomerNotFound() throws Exception {
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setCustomerId(UUID.randomUUID()); // Non-existent customer
        request.setNotes("Invoice for non-existent customer");

        mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Customer not found")));
    }

    @Test
    void testCreateInvoiceDuplicateInvoiceNumber() throws Exception {
        // First, create an invoice with a custom number
        CreateInvoiceRequest firstRequest = new CreateInvoiceRequest();
        firstRequest.setCustomerId(testCustomerId);
        firstRequest.setInvoiceNumber("DUPLICATE-001");

        mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // Try to create another invoice with the same number
        CreateInvoiceRequest secondRequest = new CreateInvoiceRequest();
        secondRequest.setCustomerId(testCustomerId);
        secondRequest.setInvoiceNumber("DUPLICATE-001");

        mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invoice number already exists")));
    }

    @Test
    void testCreateInvoiceIncrementsDraftCount() throws Exception {
        // Create an invoice
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setCustomerId(testCustomerId);
        request.setNotes("Testing draft count increment");

        mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Verify customer's draft invoice count increased
        mockMvc.perform(get("/api/customers/" + testCustomerId)
                .with(setUserContext()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.draftInvoiceCount").value(1));
    }

    @Test
    void testAutoGeneratedInvoiceNumbersAreSequential() throws Exception {
        // Create first invoice
        CreateInvoiceRequest request1 = new CreateInvoiceRequest();
        request1.setCustomerId(testCustomerId);

        MvcResult result1 = mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated())
                .andReturn();

        String invoiceNumber1 = objectMapper.readTree(result1.getResponse().getContentAsString())
                .get("data").get("invoiceNumber").asText();

        // Create second invoice
        CreateInvoiceRequest request2 = new CreateInvoiceRequest();
        request2.setCustomerId(testCustomerId);

        MvcResult result2 = mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andReturn();

        String invoiceNumber2 = objectMapper.readTree(result2.getResponse().getContentAsString())
                .get("data").get("invoiceNumber").asText();

        // Extract sequence numbers and verify they're sequential
        String seq1 = invoiceNumber1.substring(invoiceNumber1.lastIndexOf('-') + 1);
        String seq2 = invoiceNumber2.substring(invoiceNumber2.lastIndexOf('-') + 1);

        int sequence1 = Integer.parseInt(seq1);
        int sequence2 = Integer.parseInt(seq2);

        assertEquals(sequence1 + 1, sequence2, "Invoice numbers should be sequential");
    }

    // === GET BY ID TESTS ===

    @Test
    void testGetInvoiceByIdSuccess() throws Exception {
        // Create an invoice first
        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setCustomerId(testCustomerId);
        request.setNotes("Test invoice for detail");

        MvcResult createResult = mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID invoiceId = UUID.fromString(
            objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asText()
        );

        // Get the invoice by ID
        mockMvc.perform(get("/api/invoices/" + invoiceId)
                .with(setUserContext()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Invoice retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.data.customerId").value(testCustomerId.toString()))
                .andExpect(jsonPath("$.data.notes").value("Test invoice for detail"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.customerName").value("Test Invoice Customer"));
    }

    @Test
    void testGetInvoiceByIdNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/invoices/" + nonExistentId)
                .with(setUserContext()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invoice not found")));
    }

    // === LIST TESTS ===

    @Test
    void testListAllInvoices() throws Exception {
        // Create multiple invoices
        CreateInvoiceRequest request1 = new CreateInvoiceRequest();
        request1.setCustomerId(testCustomerId);
        request1.setNotes("First invoice");

        mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        CreateInvoiceRequest request2 = new CreateInvoiceRequest();
        request2.setCustomerId(testCustomerId);
        request2.setNotes("Second invoice");

        mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        // List all invoices
        mockMvc.perform(get("/api/invoices")
                .with(setUserContext()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Invoices retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void testListInvoicesByCustomer() throws Exception {
        // Create second customer
        CreateCustomerRequest customer2Request = new CreateCustomerRequest();
        customer2Request.setCompanyName("Second Customer");
        customer2Request.setEmail("second@example.com");

        MvcResult customer2Result = mockMvc.perform(post("/api/customers")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customer2Request)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID customer2Id = UUID.fromString(
            objectMapper.readTree(customer2Result.getResponse().getContentAsString())
                .get("data").get("id").asText()
        );

        // Create invoices for both customers
        CreateInvoiceRequest request1 = new CreateInvoiceRequest();
        request1.setCustomerId(testCustomerId);
        mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        CreateInvoiceRequest request2 = new CreateInvoiceRequest();
        request2.setCustomerId(customer2Id);
        mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        // List invoices for first customer only
        mockMvc.perform(get("/api/invoices")
                .param("customerId", testCustomerId.toString())
                .with(setUserContext()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].customerName").value("Test Invoice Customer"));
    }

    @Test
    void testListInvoicesEmpty() throws Exception {
        // Create a new customer with no invoices
        CreateCustomerRequest customerRequest = new CreateCustomerRequest();
        customerRequest.setCompanyName("Empty Customer");
        customerRequest.setEmail("empty@example.com");

        MvcResult customerResult = mockMvc.perform(post("/api/customers")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID emptyCustomerId = UUID.fromString(
            objectMapper.readTree(customerResult.getResponse().getContentAsString())
                .get("data").get("id").asText()
        );

        // List invoices for customer with no invoices
        mockMvc.perform(get("/api/invoices")
                .param("customerId", emptyCustomerId.toString())
                .with(setUserContext()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // === UPDATE TESTS ===

    @Test
    void testUpdateInvoiceSuccess() throws Exception {
        // Create an invoice first
        CreateInvoiceRequest createRequest = new CreateInvoiceRequest();
        createRequest.setCustomerId(testCustomerId);
        createRequest.setNotes("Original notes");

        MvcResult createResult = mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode createResponseNode =
            objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID invoiceId = UUID.fromString(createResponseNode.get("data").get("id").asText());
        Long version = createResponseNode.get("data").get("version").asLong();

        // Update the invoice
        com.invoiceme.application.invoice.dto.UpdateInvoiceRequest updateRequest =
            new com.invoiceme.application.invoice.dto.UpdateInvoiceRequest();
        updateRequest.setId(invoiceId);
        updateRequest.setVersion(version);
        updateRequest.setInvoiceNumber("UPDATED-001");
        updateRequest.setNotes("Updated notes");

        mockMvc.perform(put("/api/invoices/" + invoiceId)
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Invoice updated successfully"))
                .andExpect(jsonPath("$.data.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.data.invoiceNumber").value("UPDATED-001"))
                .andExpect(jsonPath("$.data.notes").value("Updated notes"));
    }

    @Test
    void testUpdateInvoiceOptimisticLockConflict() throws Exception {
        // Create an invoice
        CreateInvoiceRequest createRequest = new CreateInvoiceRequest();
        createRequest.setCustomerId(testCustomerId);
        createRequest.setNotes("Test notes");

        MvcResult createResult = mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode createResponseNode =
            objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID invoiceId = UUID.fromString(createResponseNode.get("data").get("id").asText());

        // Try to update with wrong version
        com.invoiceme.application.invoice.dto.UpdateInvoiceRequest updateRequest =
            new com.invoiceme.application.invoice.dto.UpdateInvoiceRequest();
        updateRequest.setId(invoiceId);
        updateRequest.setVersion(999L);  // Wrong version
        updateRequest.setNotes("Should fail");

        mockMvc.perform(put("/api/invoices/" + invoiceId)
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("modified by another user")));
    }

    @Test
    void testUpdateInvoiceNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        com.invoiceme.application.invoice.dto.UpdateInvoiceRequest updateRequest =
            new com.invoiceme.application.invoice.dto.UpdateInvoiceRequest();
        updateRequest.setId(nonExistentId);
        updateRequest.setVersion(0L);
        updateRequest.setNotes("Should fail");

        mockMvc.perform(put("/api/invoices/" + nonExistentId)
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invoice not found")));
    }

    @Test
    void testUpdateInvoiceValidationErrorMissingVersion() throws Exception {
        // Create an invoice
        CreateInvoiceRequest createRequest = new CreateInvoiceRequest();
        createRequest.setCustomerId(testCustomerId);

        MvcResult createResult = mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID invoiceId = UUID.fromString(
            objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asText()
        );

        // Try to update without version
        com.invoiceme.application.invoice.dto.UpdateInvoiceRequest updateRequest =
            new com.invoiceme.application.invoice.dto.UpdateInvoiceRequest();
        updateRequest.setId(invoiceId);
        // Missing version field
        updateRequest.setNotes("Should fail");

        mockMvc.perform(put("/api/invoices/" + invoiceId)
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // === SEND TESTS ===

    @Test
    void testMarkInvoiceAsSentSuccess() throws Exception {
        // Create an invoice first
        CreateInvoiceRequest createRequest = new CreateInvoiceRequest();
        createRequest.setCustomerId(testCustomerId);
        createRequest.setNotes("Invoice to be sent");

        MvcResult createResult = mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode createResponseNode =
            objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID invoiceId = UUID.fromString(createResponseNode.get("data").get("id").asText());

        // Manually set total > 0 for testing (in real app, this would be done via line items)
        // This is necessary because invoices must have total > 0 to be sent
        com.invoiceme.domain.invoice.Invoice invoice = invoiceRepository.findById(invoiceId).get();
        invoice.setTotal(new java.math.BigDecimal("100.00"));
        invoiceRepository.save(invoice);

        // Mark it as sent
        mockMvc.perform(post("/api/invoices/" + invoiceId + "/send")
                .with(setUserContext()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Invoice marked as sent successfully"))
                .andExpect(jsonPath("$.data.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.data.status").value("SENT"))
                .andExpect(jsonPath("$.data.invoiceDate").exists())
                .andExpect(jsonPath("$.data.invoiceDate").isNotEmpty());

        // Verify customer statistics updated
        mockMvc.perform(get("/api/customers/" + testCustomerId)
                .with(setUserContext()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.draftInvoiceCount").value(0))
                .andExpect(jsonPath("$.data.sentInvoiceCount").value(1));
    }

    @Test
    void testMarkInvoiceAsSentNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(post("/api/invoices/" + nonExistentId + "/send")
                .with(setUserContext()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invoice not found")));
    }

    @Test
    void testMarkInvoiceAsSentNotDraft() throws Exception {
        // Create and send an invoice
        CreateInvoiceRequest createRequest = new CreateInvoiceRequest();
        createRequest.setCustomerId(testCustomerId);

        MvcResult createResult = mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID invoiceId = UUID.fromString(
            objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asText()
        );

        // Manually set total > 0 for testing
        com.invoiceme.domain.invoice.Invoice invoice = invoiceRepository.findById(invoiceId).get();
        invoice.setTotal(new java.math.BigDecimal("100.00"));
        invoiceRepository.save(invoice);

        // Send it once
        mockMvc.perform(post("/api/invoices/" + invoiceId + "/send")
                .with(setUserContext()))
                .andExpect(status().isOk());

        // Try to send again - should fail
        mockMvc.perform(post("/api/invoices/" + invoiceId + "/send")
                .with(setUserContext()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Only DRAFT invoices can be sent")));
    }

    // === DELETE TESTS ===

    @Test
    void testDeleteInvoiceSuccess() throws Exception {
        // Create an invoice first
        CreateInvoiceRequest createRequest = new CreateInvoiceRequest();
        createRequest.setCustomerId(testCustomerId);
        createRequest.setNotes("Invoice to be deleted");

        MvcResult createResult = mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID invoiceId = UUID.fromString(
            objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asText()
        );

        // Delete it
        mockMvc.perform(delete("/api/invoices/" + invoiceId)
                .with(setUserContext()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Invoice deleted successfully"));

        // Verify it's gone (soft-deleted)
        mockMvc.perform(get("/api/invoices/" + invoiceId)
                .with(setUserContext()))
                .andExpect(status().isNotFound());

        // Verify customer statistics updated
        mockMvc.perform(get("/api/customers/" + testCustomerId)
                .with(setUserContext()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.draftInvoiceCount").value(0));
    }

    @Test
    void testDeleteInvoiceNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(delete("/api/invoices/" + nonExistentId)
                .with(setUserContext()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invoice not found")));
    }

    @Test
    void testDeleteInvoiceNotDraft() throws Exception {
        // Create an invoice and send it
        CreateInvoiceRequest createRequest = new CreateInvoiceRequest();
        createRequest.setCustomerId(testCustomerId);

        MvcResult createResult = mockMvc.perform(post("/api/invoices")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID invoiceId = UUID.fromString(
            objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asText()
        );

        // Manually set total > 0 and send it
        com.invoiceme.domain.invoice.Invoice invoice = invoiceRepository.findById(invoiceId).get();
        invoice.setTotal(new java.math.BigDecimal("100.00"));
        invoiceRepository.save(invoice);

        mockMvc.perform(post("/api/invoices/" + invoiceId + "/send")
                .with(setUserContext()))
                .andExpect(status().isOk());

        // Try to delete - should fail
        mockMvc.perform(delete("/api/invoices/" + invoiceId)
                .with(setUserContext()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Cannot delete SENT invoices")));
    }
}
