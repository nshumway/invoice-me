package com.invoiceme.infrastructure.persistence;

import com.invoiceme.domain.customer.Customer;
import com.invoiceme.domain.invoice.Invoice;
import com.invoiceme.domain.invoice.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    // === Standard Queries ===

    Optional<Customer> findByIdAndIsDeletedFalse(UUID id);

    List<Customer> findAllByIsDeletedFalseOrderByCompanyName();

    boolean existsByEmailAndIsDeletedFalse(String email);

    // === Custom Queries for Related Entities ===

    /**
     * Get all invoices for a customer (used for cascading updates)
     * NOTE: This will be implemented once Invoice entity exists
     */
    @Query("SELECT i FROM Invoice i WHERE i.customerId = :customerId AND i.isDeleted = false")
    List<Invoice> getInvoicesForCustomer(@Param("customerId") UUID customerId);

    /**
     * Get only SENT invoices for a customer (used for delete validation)
     * NOTE: This will be implemented once Invoice entity exists
     */
    @Query("SELECT i FROM Invoice i WHERE i.customerId = :customerId " +
           "AND i.status = :status AND i.isDeleted = false")
    List<Invoice> getSentInvoicesForCustomerByStatus(@Param("customerId") UUID customerId,
                                                      @Param("status") InvoiceStatus status);

    /**
     * Convenience method to get SENT invoices for a customer
     * NOTE: This will be implemented once Invoice entity exists
     */
    default List<Invoice> getSentInvoicesForCustomer(UUID customerId) {
        return getSentInvoicesForCustomerByStatus(customerId, InvoiceStatus.SENT);
    }
}
