package com.invoiceme.application.lineitem;

import com.invoiceme.application.lineitem.dto.LineItemResponse;
import com.invoiceme.domain.lineitem.LineItem;
import org.springframework.stereotype.Component;

/**
 * Mapper for LineItem entity to DTO conversions.
 */
@Component
public class LineItemMapper {

    /**
     * Converts LineItem entity to LineItemResponse DTO.
     * @param entity LineItem entity
     * @return LineItemResponse DTO
     */
    public LineItemResponse toResponse(LineItem entity) {
        LineItemResponse dto = new LineItemResponse();

        // Map BaseEntity fields
        dto.setId(entity.getId());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setLastModifiedAt(entity.getLastModifiedAt());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setVersion(entity.getVersion());

        // Map LineItem fields
        dto.setInvoiceId(entity.getInvoiceId());
        dto.setDescription(entity.getDescription());
        dto.setQuantity(entity.getQuantity());
        dto.setUnitPrice(entity.getUnitPrice());

        // Map read-only fields
        dto.setCustomerName(entity.getCustomerName());
        dto.setLineTotal(entity.getLineTotal());

        return dto;
    }
}
