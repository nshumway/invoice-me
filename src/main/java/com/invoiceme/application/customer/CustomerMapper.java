package com.invoiceme.application.customer;

import com.invoiceme.application.customer.dto.*;
import com.invoiceme.domain.customer.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer entity) {
        CustomerResponse dto = new CustomerResponse();

        // BaseEntity fields
        dto.setId(entity.getId());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setLastModifiedAt(entity.getLastModifiedAt());
        dto.setLastModifiedBy(entity.getLastModifiedBy());
        dto.setVersion(entity.getVersion());

        // Customer fields
        dto.setCompanyName(entity.getCompanyName());
        dto.setContactFirstName(entity.getContactFirstName());
        dto.setContactLastName(entity.getContactLastName());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setAddressLine1(entity.getAddressLine1());
        dto.setAddressLine2(entity.getAddressLine2());
        dto.setCity(entity.getCity());
        dto.setState(entity.getState());
        dto.setZipCode(entity.getZipCode());
        dto.setCountry(entity.getCountry());

        // Read-only computed fields
        dto.setDraftInvoiceCount(entity.getDraftInvoiceCount());
        dto.setSentInvoiceCount(entity.getSentInvoiceCount());
        dto.setPaidInvoiceCount(entity.getPaidInvoiceCount());
        dto.setTotalOutstanding(entity.getTotalOutstanding());

        return dto;
    }

    public CustomerListItemResponse toListItem(Customer entity) {
        CustomerListItemResponse dto = new CustomerListItemResponse();

        dto.setId(entity.getId());
        dto.setCompanyName(entity.getCompanyName());
        dto.setEmail(entity.getEmail());
        dto.setTotalOutstanding(entity.getTotalOutstanding());

        return dto;
    }
}
