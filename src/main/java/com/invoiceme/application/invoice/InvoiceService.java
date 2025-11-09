package com.invoiceme.application.invoice;

import com.invoiceme.application.invoice.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Mock InvoiceService - will be fully implemented in Phase 3
 * This is a placeholder to satisfy Customer entity dependencies
 */
@Service
public class InvoiceService {

    @Transactional(propagation = Propagation.MANDATORY)
    public InvoiceResponse systemUpdateInvoice(UpdateInvoiceRequest request) {
        // TODO: Implement when building Invoice entity
        // For now, just a placeholder that does nothing
        return new InvoiceResponse();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void systemDeleteInvoice(UUID invoiceId) {
        // TODO: Implement when building Invoice entity
        // For now, just a placeholder that does nothing
    }
}
