package com.invoiceme.application.customer;

import com.invoiceme.application.customer.dto.CustomerListItemResponse;
import com.invoiceme.application.customer.dto.CustomerResponse;
import com.invoiceme.domain.customer.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CustomerMapperTest {

    private CustomerMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CustomerMapper();
    }

    @Test
    void testToResponseMapsAllFields() {
        Customer customer = new Customer();
        customer.setCompanyName("Acme Corp");
        customer.setContactFirstName("John");
        customer.setContactLastName("Doe");
        customer.setEmail("john@acme.com");
        customer.setPhone("555-1234");
        customer.setAddressLine1("123 Main St");
        customer.setAddressLine2("Suite 100");
        customer.setCity("Boston");
        customer.setState("MA");
        customer.setZipCode("02101");
        customer.setCountry("USA");
        customer.setDraftInvoiceCount(5);
        customer.setSentInvoiceCount(10);
        customer.setPaidInvoiceCount(8);
        customer.setTotalOutstanding(new BigDecimal("1500.50"));

        CustomerResponse response = mapper.toResponse(customer);

        // Test business fields
        assertEquals("Acme Corp", response.getCompanyName());
        assertEquals("John", response.getContactFirstName());
        assertEquals("Doe", response.getContactLastName());
        assertEquals("john@acme.com", response.getEmail());
        assertEquals("555-1234", response.getPhone());
        assertEquals("123 Main St", response.getAddressLine1());
        assertEquals("Suite 100", response.getAddressLine2());
        assertEquals("Boston", response.getCity());
        assertEquals("MA", response.getState());
        assertEquals("02101", response.getZipCode());
        assertEquals("USA", response.getCountry());
        assertEquals(5, response.getDraftInvoiceCount());
        assertEquals(10, response.getSentInvoiceCount());
        assertEquals(8, response.getPaidInvoiceCount());
        assertEquals(new BigDecimal("1500.50"), response.getTotalOutstanding());

        // Note: BaseEntity fields (id, createdAt, createdBy, etc.) are tested
        // in integration tests where JPA lifecycle callbacks populate them
    }

    @Test
    void testToResponseHandlesNullOptionalFields() {
        Customer customer = new Customer();
        customer.setCompanyName("Minimal Corp");
        customer.setEmail("contact@minimal.com");
        customer.setDraftInvoiceCount(0);
        customer.setSentInvoiceCount(0);
        customer.setPaidInvoiceCount(0);
        customer.setTotalOutstanding(BigDecimal.ZERO);

        CustomerResponse response = mapper.toResponse(customer);

        assertEquals("Minimal Corp", response.getCompanyName());
        assertEquals("contact@minimal.com", response.getEmail());
        assertNull(response.getContactFirstName());
        assertNull(response.getContactLastName());
        assertNull(response.getPhone());
        assertNull(response.getAddressLine1());
        assertNull(response.getAddressLine2());
        assertNull(response.getCity());
        assertNull(response.getState());
        assertNull(response.getZipCode());
        assertNull(response.getCountry());
    }

    @Test
    void testToListItemMapsCorrectFields() {
        Customer customer = new Customer();
        customer.setCompanyName("List Test Corp");
        customer.setEmail("list@test.com");
        customer.setTotalOutstanding(new BigDecimal("2500.75"));

        CustomerListItemResponse response = mapper.toListItem(customer);

        assertEquals("List Test Corp", response.getCompanyName());
        assertEquals("list@test.com", response.getEmail());
        assertEquals(new BigDecimal("2500.75"), response.getTotalOutstanding());
    }

    @Test
    void testToListItemHandlesZeroOutstanding() {
        Customer customer = new Customer();
        customer.setCompanyName("Zero Outstanding");
        customer.setEmail("zero@test.com");
        customer.setTotalOutstanding(BigDecimal.ZERO);

        CustomerListItemResponse response = mapper.toListItem(customer);

        assertEquals(BigDecimal.ZERO, response.getTotalOutstanding());
    }

    @Test
    void testToListItemHandlesNullEmail() {
        Customer customer = new Customer();
        customer.setCompanyName("No Email Corp");
        customer.setEmail(null);
        customer.setTotalOutstanding(BigDecimal.ZERO);

        CustomerListItemResponse response = mapper.toListItem(customer);

        assertNull(response.getEmail());
    }
}
