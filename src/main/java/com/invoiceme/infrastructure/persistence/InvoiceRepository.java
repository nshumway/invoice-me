package com.invoiceme.infrastructure.persistence;

import com.invoiceme.domain.invoice.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
     * Find all non-deleted invoices for a specific customer.
     */
    List<Invoice> findAllByCustomerIdAndIsDeletedFalse(UUID customerId);

    /**
     * Find invoice numbers that start with a specific prefix.
     * Used for generating sequential invoice numbers.
     * @param prefix The invoice number prefix (e.g., "INV-2025-11-09-")
     * @return List of invoice numbers matching the prefix
     */
    @Query("SELECT i.invoiceNumber FROM Invoice i WHERE i.invoiceNumber LIKE CONCAT(:prefix, '%') AND i.isDeleted = false")
    List<String> findInvoiceNumbersByPrefix(@Param("prefix") String prefix);
}
