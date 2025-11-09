package com.invoiceme.infrastructure.persistence;

import com.invoiceme.domain.customer.Customer;
import com.invoiceme.domain.invoice.Invoice;
import com.invoiceme.domain.invoice.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    // === Atomic Statistics Updates (Fix for Issue #4: Race Conditions) ===

    /**
     * Atomically increment draft invoice count using database-level operation.
     * This prevents race conditions when multiple invoices are created concurrently.
     */
    @Modifying
    @Query("UPDATE Customer c SET c.draftInvoiceCount = c.draftInvoiceCount + 1 " +
           "WHERE c.id = :customerId AND c.isDeleted = false")
    int incrementDraftInvoiceCount(@Param("customerId") UUID customerId);

    /**
     * Atomically decrement draft invoice count using database-level operation.
     */
    @Modifying
    @Query("UPDATE Customer c SET c.draftInvoiceCount = c.draftInvoiceCount - 1 " +
           "WHERE c.id = :customerId AND c.isDeleted = false AND c.draftInvoiceCount > 0")
    int decrementDraftInvoiceCount(@Param("customerId") UUID customerId);

    /**
     * Atomically increment sent invoice count using database-level operation.
     */
    @Modifying
    @Query("UPDATE Customer c SET c.sentInvoiceCount = c.sentInvoiceCount + 1 " +
           "WHERE c.id = :customerId AND c.isDeleted = false")
    int incrementSentInvoiceCount(@Param("customerId") UUID customerId);

    /**
     * Atomically decrement sent invoice count using database-level operation.
     */
    @Modifying
    @Query("UPDATE Customer c SET c.sentInvoiceCount = c.sentInvoiceCount - 1 " +
           "WHERE c.id = :customerId AND c.isDeleted = false AND c.sentInvoiceCount > 0")
    int decrementSentInvoiceCount(@Param("customerId") UUID customerId);

    /**
     * Atomically increment paid invoice count using database-level operation.
     */
    @Modifying
    @Query("UPDATE Customer c SET c.paidInvoiceCount = c.paidInvoiceCount + 1 " +
           "WHERE c.id = :customerId AND c.isDeleted = false")
    int incrementPaidInvoiceCount(@Param("customerId") UUID customerId);

    /**
     * Atomically decrement paid invoice count using database-level operation.
     */
    @Modifying
    @Query("UPDATE Customer c SET c.paidInvoiceCount = c.paidInvoiceCount - 1 " +
           "WHERE c.id = :customerId AND c.isDeleted = false AND c.paidInvoiceCount > 0")
    int decrementPaidInvoiceCount(@Param("customerId") UUID customerId);

    /**
     * Atomically add to total outstanding amount using database-level operation.
     */
    @Modifying
    @Query("UPDATE Customer c SET c.totalOutstanding = c.totalOutstanding + :amount " +
           "WHERE c.id = :customerId AND c.isDeleted = false")
    int addToTotalOutstanding(@Param("customerId") UUID customerId, @Param("amount") BigDecimal amount);

    /**
     * Atomically subtract from total outstanding amount using database-level operation.
     */
    @Modifying
    @Query("UPDATE Customer c SET c.totalOutstanding = c.totalOutstanding - :amount " +
           "WHERE c.id = :customerId AND c.isDeleted = false")
    int subtractFromTotalOutstanding(@Param("customerId") UUID customerId, @Param("amount") BigDecimal amount);
}
