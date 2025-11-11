package com.invoiceme.infrastructure.persistence;

import com.invoiceme.domain.invoice.Invoice;
import com.invoiceme.domain.invoice.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    /**
     * Find invoice by ID, excluding soft-deleted records.
     */
    Optional<Invoice> findByIdAndIsDeletedFalse(UUID id);

    /**
     * Check if invoice number exists (excluding soft-deleted records).
     */
    boolean existsByInvoiceNumberAndIsDeletedFalse(String invoiceNumber);

    /**
     * Find all non-deleted invoices, ordered by invoice date descending.
     */
    List<Invoice> findAllByIsDeletedFalseOrderByInvoiceDateDesc();

    /**
     * Find all non-deleted invoices for the current user, ordered by invoice date descending.
     */
    List<Invoice> findAllByCreatedByAndIsDeletedFalseOrderByInvoiceDateDesc(UUID createdBy);

    /**
     * Find all non-deleted invoices for a specific customer.
     */
    List<Invoice> findAllByCustomerIdAndIsDeletedFalse(UUID customerId);

    /**
     * Find all non-deleted invoices by status, ordered by invoice date descending.
     */
    List<Invoice> findAllByStatusAndIsDeletedFalseOrderByInvoiceDateDesc(InvoiceStatus status);

    /**
     * Find all non-deleted invoices by status for the current user, ordered by invoice date descending.
     */
    List<Invoice> findAllByCreatedByAndStatusAndIsDeletedFalseOrderByInvoiceDateDesc(UUID createdBy, InvoiceStatus status);

    /**
     * Find all non-deleted invoices for a specific customer by status.
     */
    List<Invoice> findAllByCustomerIdAndStatusAndIsDeletedFalse(UUID customerId, InvoiceStatus status);

    /**
     * Find invoice numbers that start with a specific prefix.
     * Used for generating sequential invoice numbers.
     * @param prefix The invoice number prefix (e.g., "INV-2025-11-09-")
     * @return List of invoice numbers matching the prefix
     */
    @Query("SELECT i.invoiceNumber FROM Invoice i WHERE i.invoiceNumber LIKE CONCAT(:prefix, '%') AND i.isDeleted = false")
    List<String> findInvoiceNumbersByPrefix(@Param("prefix") String prefix);

    /**
     * Count invoices for a customer by status.
     * @param customerId The customer ID
     * @param status The invoice status
     * @return Count of invoices
     */
    Integer countByCustomerIdAndStatusAndIsDeletedFalse(UUID customerId, InvoiceStatus status);

    /**
     * Calculate total outstanding balance for a customer.
     * Sums the balance (total - amountPaid) of all SENT and PAID invoices.
     * @param customerId The customer ID
     * @return Total outstanding balance
     */
    @Query("""
        SELECT COALESCE(SUM(i.total - i.amountPaid), 0)
        FROM Invoice i
        WHERE i.customerId = :customerId
        AND i.status IN ('SENT', 'PAID')
        AND i.isDeleted = false
        AND (i.total - i.amountPaid) > 0
        """)
    BigDecimal calculateTotalOutstanding(@Param("customerId") UUID customerId);
}
