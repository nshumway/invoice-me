package com.invoiceme.infrastructure.web;

import com.invoiceme.application.common.ApiResponse;
import com.invoiceme.application.payment.PaymentService;
import com.invoiceme.application.payment.dto.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices/{invoiceId}/payments")
@Tag(name = "Payments", description = "Payment recording and retrieval endpoints")
@SecurityRequirement(name = "bearerAuth")
public class InvoicePaymentController {

    @Autowired
    private PaymentService paymentService;

    // === LIST PAYMENTS FOR INVOICE ===

    @Operation(
        summary = "List payments for an invoice",
        description = "Retrieves all payments for a specific invoice, ordered by payment date ascending"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Payments retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = PaymentResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Unauthorized - invalid or missing JWT token",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> listPaymentsForInvoice(
            @Parameter(description = "Invoice UUID", required = true)
            @PathVariable UUID invoiceId) {

        List<PaymentResponse> response = paymentService.listPaymentsForInvoice(invoiceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
