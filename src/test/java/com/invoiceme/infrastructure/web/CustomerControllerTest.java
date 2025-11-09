package com.invoiceme.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceme.application.common.UserContext;
import com.invoiceme.application.customer.dto.*;
import com.invoiceme.infrastructure.persistence.CustomerRepository;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser  // Bypasses JWT authentication for testing
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    private UUID testUserId;

    // Request post processor to set UserContext for each request
    private RequestPostProcessor setUserContext() {
        return request -> {
            UserContext.setCurrentUser(testUserId);
            return request;
        };
    }

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // === CREATE TESTS ===

    @Test
    void testCreateCustomerSuccess() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("API Test Corp");
        request.setEmail("api@test.com");
        request.setContactFirstName("John");
        request.setContactLastName("Doe");
        request.setPhone("555-1234");
        request.setAddressLine1("123 Main St");
        request.setCity("Boston");
        request.setState("MA");
        request.setZipCode("02101");
        request.setCountry("USA");

        mockMvc.perform(post("/api/customers")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Customer created successfully"))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.companyName").value("API Test Corp"))
                .andExpect(jsonPath("$.data.email").value("api@test.com"))
                .andExpect(jsonPath("$.data.contactFirstName").value("John"))
                .andExpect(jsonPath("$.data.contactLastName").value("Doe"))
                .andExpect(jsonPath("$.data.phone").value("555-1234"))
                .andExpect(jsonPath("$.data.draftInvoiceCount").value(0))
                .andExpect(jsonPath("$.data.sentInvoiceCount").value(0))
                .andExpect(jsonPath("$.data.paidInvoiceCount").value(0))
                .andExpect(jsonPath("$.data.totalOutstanding").value(0))
                .andExpect(jsonPath("$.data.version").value(0));
    }

    @Test
    void testCreateCustomerValidationErrorMissingCompanyName() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest();
        // Missing company name
        request.setEmail("test@example.com");

        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testCreateCustomerValidationErrorMissingEmail() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName("Test Corp");
        // Missing email

        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testCreateCustomerDuplicateEmail() throws Exception {
        // Create first customer
        CreateCustomerRequest request1 = new CreateCustomerRequest();
        request1.setCompanyName("First Corp");
        request1.setEmail("duplicate@test.com");

        mockMvc.perform(post("/api/customers")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Try to create second customer with same email
        CreateCustomerRequest request2 = new CreateCustomerRequest();
        request2.setCompanyName("Second Corp");
        request2.setEmail("duplicate@test.com");

        mockMvc.perform(post("/api/customers")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    // === UPDATE TESTS ===

    @Test
    void testUpdateCustomerSuccess() throws Exception {
        // Create customer first
        CreateCustomerRequest createRequest = new CreateCustomerRequest();
        createRequest.setCompanyName("Original Corp");
        createRequest.setEmail("original@test.com");

        String createResponse = mockMvc.perform(post("/api/customers")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CustomerResponse created = objectMapper.readTree(createResponse)
                .get("data")
                .traverse(objectMapper)
                .readValueAs(CustomerResponse.class);

        // Update customer
        UpdateCustomerRequest updateRequest = new UpdateCustomerRequest();
        updateRequest.setVersion(created.getVersion());
        updateRequest.setCompanyName("Updated Corp");
        updateRequest.setEmail("updated@test.com");
        updateRequest.setContactFirstName("Jane");

        mockMvc.perform(put("/api/customers/" + created.getId())
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Customer updated successfully"))
                .andExpect(jsonPath("$.data.companyName").value("Updated Corp"))
                .andExpect(jsonPath("$.data.email").value("updated@test.com"))
                .andExpect(jsonPath("$.data.contactFirstName").value("Jane"))
                .andExpect(jsonPath("$.data.version").value(1));
    }

    @Test
    void testUpdateNonExistentCustomer() throws Exception {
        UpdateCustomerRequest updateRequest = new UpdateCustomerRequest();
        updateRequest.setVersion(0L);
        updateRequest.setCompanyName("Test Corp");
        updateRequest.setEmail("test@test.com");

        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(put("/api/customers/" + nonExistentId)
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Customer not found"));
    }

    // === DELETE TESTS ===

    @Test
    void testDeleteCustomerSuccess() throws Exception {
        // Create customer first
        CreateCustomerRequest createRequest = new CreateCustomerRequest();
        createRequest.setCompanyName("To Delete Corp");
        createRequest.setEmail("delete@test.com");

        String createResponse = mockMvc.perform(post("/api/customers")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CustomerResponse created = objectMapper.readTree(createResponse)
                .get("data")
                .traverse(objectMapper)
                .readValueAs(CustomerResponse.class);

        // Delete customer
        mockMvc.perform(delete("/api/customers/" + created.getId())
                .with(setUserContext())
                .param("version", created.getVersion().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Customer deleted successfully"));

        // Verify customer is deleted (should return 404)
        mockMvc.perform(get("/api/customers/" + created.getId())
                .with(setUserContext()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteNonExistentCustomer() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(delete("/api/customers/" + nonExistentId)
                .with(setUserContext())
                .param("version", "0"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Customer not found"));
    }

    // === GET BY ID TESTS ===

    @Test
    void testGetCustomerByIdSuccess() throws Exception {
        // Create customer first
        CreateCustomerRequest createRequest = new CreateCustomerRequest();
        createRequest.setCompanyName("Get Test Corp");
        createRequest.setEmail("get@test.com");
        createRequest.setContactFirstName("John");

        String createResponse = mockMvc.perform(post("/api/customers")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CustomerResponse created = objectMapper.readTree(createResponse)
                .get("data")
                .traverse(objectMapper)
                .readValueAs(CustomerResponse.class);

        // Get customer by ID
        mockMvc.perform(get("/api/customers/" + created.getId())
                .with(setUserContext()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(created.getId().toString()))
                .andExpect(jsonPath("$.data.companyName").value("Get Test Corp"))
                .andExpect(jsonPath("$.data.email").value("get@test.com"))
                .andExpect(jsonPath("$.data.contactFirstName").value("John"));
    }

    @Test
    void testGetNonExistentCustomer() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/api/customers/" + nonExistentId)
                .with(setUserContext()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Customer not found"));
    }

    // === LIST ALL TESTS ===

    @Test
    void testListAllCustomersSuccess() throws Exception {
        // Create multiple customers
        createCustomer("Alpha Corp", "alpha@test.com");
        createCustomer("Beta Corp", "beta@test.com");
        createCustomer("Gamma Corp", "gamma@test.com");

        mockMvc.perform(get("/api/customers")
                .with(setUserContext()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(3))));
    }

    @Test
    void testListAllCustomersExcludesDeleted() throws Exception {
        // Create customer
        CreateCustomerRequest createRequest = new CreateCustomerRequest();
        createRequest.setCompanyName("To Delete in List Test");
        createRequest.setEmail("deleteinlist@test.com");

        String createResponse = mockMvc.perform(post("/api/customers")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CustomerResponse created = objectMapper.readTree(createResponse)
                .get("data")
                .traverse(objectMapper)
                .readValueAs(CustomerResponse.class);

        // List before delete - should be present
        mockMvc.perform(get("/api/customers")
                .with(setUserContext()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.companyName == 'To Delete in List Test')]").exists());

        // Delete customer
        mockMvc.perform(delete("/api/customers/" + created.getId())
                .with(setUserContext())
                .param("version", created.getVersion().toString()))
                .andExpect(status().isOk());

        // List after delete - should not be present
        mockMvc.perform(get("/api/customers")
                .with(setUserContext()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.companyName == 'To Delete in List Test')]").doesNotExist());
    }

    // === HELPER METHODS ===

    private void createCustomer(String companyName, String email) throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest();
        request.setCompanyName(companyName);
        request.setEmail(email);

        mockMvc.perform(post("/api/customers")
                .with(setUserContext())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
