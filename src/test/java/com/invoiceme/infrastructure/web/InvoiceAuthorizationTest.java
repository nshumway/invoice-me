package com.invoiceme.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Authorization tests for Invoice API endpoints.
 * Verifies that all endpoints require authentication.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class InvoiceAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetAllInvoices_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetInvoiceById_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/invoices/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreateInvoice_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/invoices")
                        .contentType("application/json")
                        .content("{\"customerId\":\"00000000-0000-0000-0000-000000000000\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testUpdateInvoice_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(put("/api/invoices/00000000-0000-0000-0000-000000000000")
                        .contentType("application/json")
                        .content("{\"notes\":\"test\",\"version\":0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testMarkAsSent_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/invoices/00000000-0000-0000-0000-000000000000/send")
                        .contentType("application/json")
                        .content("{\"version\":0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testDeleteInvoice_withoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(delete("/api/invoices/00000000-0000-0000-0000-000000000000?version=0"))
                .andExpect(status().isForbidden());
    }
}
