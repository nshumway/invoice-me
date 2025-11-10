package com.invoiceme.infrastructure.persistence;

import com.invoiceme.domain.lineitem.LineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for LineItem entity.
 */
@Repository
public interface LineItemRepository extends JpaRepository<LineItem, UUID> {

    /**
     * Finds a line item by ID, excluding soft-deleted items.
     * @param id Line item ID
     * @return Optional containing the line item if found and not deleted
     */
    Optional<LineItem> findByIdAndIsDeletedFalse(UUID id);

    /**
     * Finds all line items for an invoice, ordered by creation date.
     * @param invoiceId Invoice ID
     * @return List of line items for the invoice
     */
    List<LineItem> findAllByInvoiceIdAndIsDeletedFalseOrderByCreatedAtAsc(UUID invoiceId);

    /**
     * Sums the line totals for all line items in an invoice.
     * Used for invoice total recalculation.
     * @param invoiceId Invoice ID
     * @return Sum of line totals, or null if no line items exist
     */
    @Query("SELECT SUM(l.lineTotal) FROM LineItem l WHERE l.invoiceId = :invoiceId AND l.isDeleted = false")
    BigDecimal sumLineTotalsByInvoiceId(@Param("invoiceId") UUID invoiceId);
}
