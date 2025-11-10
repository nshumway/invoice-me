package com.invoiceme.application.invoice;

import com.invoiceme.application.invoice.dto.InvoiceListItemResponse;
import com.invoiceme.application.invoice.dto.InvoiceResponse;
import com.invoiceme.domain.invoice.Invoice;
import org.springframework.stereotype.Component;

/**
 * Maps between Invoice entity and DTOs.
 */
@Component
public class InvoiceMapper {

    /**
     * Maps Invoice entity to full InvoiceResponse DTO.
     * Includes all fields and calculates balance.
     * @param entity The invoice entity
     * @return Invoice response DTO
     */
    public InvoiceResponse toResponse(Invoice entity) {
        if (entity == null) {
            return null;
        }

        InvoiceResponse dto = new InvoiceResponse();

        // BaseEntity fields
        dto.setId(entity.getId());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setLastModifiedAt(entity.getLastModifiedAt());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setVersion(entity.getVersion());

        // Invoice fields
        dto.setCustomerId(entity.getCustomerId());
        dto.setInvoiceNumber(entity.getInvoiceNumber());
        dto.setNotes(entity.getNotes());

        // Read-only fields
        dto.setInvoiceDate(entity.getInvoiceDate());
        dto.setStatus(entity.getStatus());
        dto.setCustomerName(entity.getCustomerName());
        dto.setTotal(entity.getTotal());
        dto.setAmountPaid(entity.getAmountPaid());

        // Calculated field
        dto.setBalance(entity.getBalance());

        return dto;
    }

    /**
     * Maps Invoice entity to InvoiceListItemResponse DTO.
     * Includes only essential fields for list views.
     * @param entity The invoice entity
     * @return Invoice list item response DTO
     */
    public InvoiceListItemResponse toListItem(Invoice entity) {
        if (entity == null) {
            return null;
        }

        InvoiceListItemResponse dto = new InvoiceListItemResponse();

        dto.setId(entity.getId());
        dto.setInvoiceNumber(entity.getInvoiceNumber());
        dto.setStatus(entity.getStatus());
        dto.setCustomerName(entity.getCustomerName());
        dto.setTotal(entity.getTotal());
        dto.setAmountPaid(entity.getAmountPaid());

        // Calculated field
        dto.setBalance(entity.getBalance());

        return dto;
    }
}
