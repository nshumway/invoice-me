package com.invoiceme.infrastructure.persistence;

import com.invoiceme.domain.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Find a payment by ID that is not deleted.
     */
    Optional<Payment> findByIdAndIsDeletedFalse(UUID id);

    /**
     * Find all payments for an invoice (not deleted), ordered by payment date ascending.
     */
    List<Payment> findAllByInvoiceIdAndIsDeletedFalseOrderByPaymentDateAsc(UUID invoiceId);

    /**
     * Calculate the sum of all payment amounts for an invoice.
     * Returns null if no payments exist.
     */
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.invoiceId = :invoiceId AND p.isDeleted = false")
    BigDecimal sumAmountsByInvoiceId(@Param("invoiceId") UUID invoiceId);
}
