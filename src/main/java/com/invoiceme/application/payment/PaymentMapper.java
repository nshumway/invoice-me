package com.invoiceme.application.payment;

import com.invoiceme.application.payment.dto.PaymentResponse;
import com.invoiceme.domain.payment.Payment;
import org.springframework.stereotype.Component;

/**
 * Maps between Payment entity and DTOs.
 */
@Component
public class PaymentMapper {

    /**
     * Maps Payment entity to PaymentResponse DTO.
     * @param entity The payment entity
     * @return Payment response DTO
     */
    public PaymentResponse toResponse(Payment entity) {
        if (entity == null) {
            return null;
        }

        PaymentResponse dto = new PaymentResponse();

        // BaseEntity fields
        dto.setId(entity.getId());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setLastModifiedAt(entity.getLastModifiedAt());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setVersion(entity.getVersion());

        // Payment fields
        dto.setInvoiceId(entity.getInvoiceId());
        dto.setPaymentDate(entity.getPaymentDate());
        dto.setAmount(entity.getAmount());
        dto.setPaymentMethod(entity.getPaymentMethod());
        dto.setReferenceNumber(entity.getReferenceNumber());
        dto.setNotes(entity.getNotes());

        // Read-only fields
        dto.setCustomerName(entity.getCustomerName());

        return dto;
    }
}
