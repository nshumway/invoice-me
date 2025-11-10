package com.invoiceme.infrastructure.web;

import com.invoiceme.application.common.ApiResponse;
import com.invoiceme.application.lineitem.LineItemService;
import com.invoiceme.application.lineitem.dto.CreateLineItemRequest;
import com.invoiceme.application.lineitem.dto.LineItemResponse;
import com.invoiceme.application.lineitem.dto.UpdateLineItemRequest;
import com.invoiceme.domain.common.exceptions.ValidationException;
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
 * REST controller for LineItem operations.
 * Provides endpoints for creating, reading, updating, and deleting line items.
 * Line items are nested under invoices: /api/invoices/{invoiceId}/line-items
 */
@RestController
@RequestMapping("/api/invoices/{invoiceId}/line-items")
@Tag(name = "Line Items", description = "Line item management operations for invoices")
@SecurityRequirement(name = "bearerAuth")
public class LineItemController {

    @Autowired
    private LineItemService lineItemService;

    // === CREATE ===

    @Operation(
        summary = "Add a line item to an invoice",
        description = "Creates a new line item for the specified invoice. " +
                     "Invoice must be in DRAFT status. " +
                     "Line total is automatically calculated as quantity × unit price. " +
                     "Invoice total is automatically recalculated after the line item is added."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Line item successfully created",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = LineItemResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input or validation error (e.g., invoice is not DRAFT, quantity ≤ 0, price ≤ 0)",
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
    @PostMapping
    public ResponseEntity<ApiResponse<LineItemResponse>> createLineItem(
            @Parameter(description = "Invoice ID") @PathVariable UUID invoiceId,
            @Valid @RequestBody CreateLineItemRequest request) {

        // Ensure invoiceId in path matches request
        if (!invoiceId.equals(request.getInvoiceId())) {
            throw new ValidationException("Invoice ID mismatch");
        }

        LineItemResponse response = lineItemService.createLineItem(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Line item added successfully", response));
    }

    // === LIST ===

    @Operation(
        summary = "List all line items for an invoice",
        description = "Retrieves all line items for the specified invoice, ordered by creation date (oldest first)"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Line items retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = LineItemResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Unauthorized - invalid or missing JWT token",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<LineItemResponse>>> listLineItemsForInvoice(
            @Parameter(description = "Invoice ID") @PathVariable UUID invoiceId) {

        List<LineItemResponse> response = lineItemService.listLineItemsForInvoice(invoiceId);
        return ResponseEntity
                .ok(ApiResponse.success("Line items retrieved successfully", response));
    }

    // === UPDATE ===

    @Operation(
        summary = "Update a line item",
        description = "Updates line item fields (description, quantity, unit price). " +
                     "Only allowed for line items on DRAFT invoices. " +
                     "Line total is automatically recalculated. " +
                     "Invoice total is automatically recalculated after the update. " +
                     "Requires version number for optimistic locking."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Line item updated successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = LineItemResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input or validation error (e.g., invoice is not DRAFT, quantity ≤ 0, price ≤ 0)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Line item not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Optimistic locking conflict - line item was modified by another user",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Unauthorized - invalid or missing JWT token",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    @PutMapping("/{lineItemId}")
    public ResponseEntity<ApiResponse<LineItemResponse>> updateLineItem(
            @Parameter(description = "Invoice ID") @PathVariable UUID invoiceId,
            @Parameter(description = "Line Item ID") @PathVariable UUID lineItemId,
            @Valid @RequestBody UpdateLineItemRequest request) {

        // Ensure IDs match
        if (!lineItemId.equals(request.getId())) {
            throw new ValidationException("Line item ID mismatch");
        }

        LineItemResponse response = lineItemService.updateLineItem(request);
        return ResponseEntity
                .ok(ApiResponse.success("Line item updated successfully", response));
    }

    // === DELETE ===

    @Operation(
        summary = "Delete a line item",
        description = "Soft-deletes a line item from the invoice. " +
                     "Only allowed for line items on DRAFT invoices. " +
                     "Invoice total is automatically recalculated after deletion. " +
                     "Requires version number for optimistic locking."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Line item deleted successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input or validation error (e.g., invoice is not DRAFT)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Line item not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Optimistic locking conflict - line item was modified by another user",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Unauthorized - invalid or missing JWT token",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    @DeleteMapping("/{lineItemId}")
    public ResponseEntity<ApiResponse<Void>> deleteLineItem(
            @Parameter(description = "Invoice ID") @PathVariable UUID invoiceId,
            @Parameter(description = "Line Item ID") @PathVariable UUID lineItemId,
            @Parameter(description = "Version for optimistic locking") @RequestParam Long version) {

        lineItemService.deleteLineItem(lineItemId, version);
        return ResponseEntity
                .ok(ApiResponse.success("Line item deleted successfully", null));
    }
}
