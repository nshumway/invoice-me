package com.invoiceme.infrastructure.web;

import com.invoiceme.application.common.ApiResponse;
import com.invoiceme.application.invoice.InvoiceService;
import com.invoiceme.application.invoice.dto.CreateInvoiceRequest;
import com.invoiceme.application.invoice.dto.InvoiceListItemResponse;
import com.invoiceme.application.invoice.dto.InvoiceResponse;
import com.invoiceme.application.invoice.dto.UpdateInvoiceRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Invoice operations.
 * Provides endpoints for creating, reading, updating, and deleting invoices.
 */
@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoices", description = "Invoice management operations")
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    // === CREATE ===

    @Operation(
        summary = "Create a new invoice",
        description = "Creates a new invoice in DRAFT status for the specified customer. " +
                     "Invoice number can be provided or will be auto-generated in format INV-YYYY-MM-DD-###."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Invoice successfully created",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = InvoiceResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input or validation error (e.g., duplicate invoice number)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Customer not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Unauthorized - invalid or missing JWT token",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request) {

        InvoiceResponse response = invoiceService.createInvoice(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Invoice created successfully", response));
    }

    // === GET BY ID ===

    @Operation(
        summary = "Get invoice by ID",
        description = "Retrieves detailed information for a specific invoice"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Invoice retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = InvoiceResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Invoice not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Unauthorized - invalid or missing JWT token",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(
            @Parameter(description = "Invoice ID") @PathVariable UUID id) {

        InvoiceResponse response = invoiceService.getInvoiceById(id);
        return ResponseEntity
                .ok(ApiResponse.success("Invoice retrieved successfully", response));
    }

    // === UPDATE ===

    @Operation(
        summary = "Update an invoice",
        description = "Updates invoice fields. Only allowed for DRAFT status invoices. " +
                     "Requires version number for optimistic locking."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Invoice updated successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = InvoiceResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input or validation error (e.g., trying to edit non-DRAFT invoice)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Invoice not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Optimistic locking conflict - invoice was modified by another user",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Unauthorized - invalid or missing JWT token",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> updateInvoice(
            @Parameter(description = "Invoice ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateInvoiceRequest request) {

        // Ensure ID in path matches ID in request body
        request.setId(id);

        InvoiceResponse response = invoiceService.updateInvoice(request);
        return ResponseEntity
                .ok(ApiResponse.success("Invoice updated successfully", response));
    }

    // === SEND ===

    @Operation(
        summary = "Mark invoice as sent",
        description = "Transitions invoice from DRAFT to SENT status. Sets invoice date to current timestamp. " +
                     "Only DRAFT invoices can be marked as sent."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Invoice marked as sent successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = InvoiceResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid operation - invoice is not in DRAFT status",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Invoice not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Unauthorized - invalid or missing JWT token",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    @PostMapping("/{id}/send")
    public ResponseEntity<ApiResponse<InvoiceResponse>> markInvoiceAsSent(
            @Parameter(description = "Invoice ID") @PathVariable UUID id) {

        InvoiceResponse response = invoiceService.markInvoiceAsSent(id);
        return ResponseEntity
                .ok(ApiResponse.success("Invoice marked as sent successfully", response));
    }

    // === DELETE ===

    @Operation(
        summary = "Delete an invoice",
        description = "Soft-deletes an invoice. Only DRAFT invoices can be deleted."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Invoice deleted successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid operation - invoice is not in DRAFT status",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Invoice not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Unauthorized - invalid or missing JWT token",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(
            @Parameter(description = "Invoice ID") @PathVariable UUID id) {

        invoiceService.deleteInvoice(id);
        return ResponseEntity
                .ok(ApiResponse.success("Invoice deleted successfully", null));
    }

    // === LIST ===

    @Operation(
        summary = "List all invoices",
        description = "Retrieves all invoices, sorted by invoice date descending (most recent first). " +
                     "Can optionally filter by customer ID."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Invoices retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = InvoiceListItemResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Unauthorized - invalid or missing JWT token",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceListItemResponse>>> listInvoices(
            @Parameter(description = "Optional customer ID to filter invoices")
            @RequestParam(required = false) UUID customerId) {

        List<InvoiceListItemResponse> invoices;

        if (customerId != null) {
            invoices = invoiceService.listInvoicesByCustomer(customerId);
        } else {
            invoices = invoiceService.listAllInvoices();
        }

        return ResponseEntity
                .ok(ApiResponse.success("Invoices retrieved successfully", invoices));
    }
}
