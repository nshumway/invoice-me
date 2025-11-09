package com.invoiceme.infrastructure.web;

import com.invoiceme.application.common.ApiResponse;
import com.invoiceme.application.customer.CustomerService;
import com.invoiceme.application.customer.dto.*;
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

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customers", description = "Customer management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    // === CREATE ===

    @Operation(
        summary = "Create a new customer",
        description = "Creates a new customer record with the provided information"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Customer successfully created",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = CustomerResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input or validation error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Unauthorized - invalid or missing JWT token",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {

        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created successfully", response));
    }

    // === UPDATE ===

    @Operation(
        summary = "Update an existing customer",
        description = "Updates a customer's information. Requires version number for optimistic locking."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Customer successfully updated",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = CustomerResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid input or validation error",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Customer not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Optimistic locking conflict - customer was modified by another user",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Unauthorized - invalid or missing JWT token",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @Parameter(description = "Customer UUID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request) {

        request.setId(id); // Ensure ID from path
        CustomerResponse response = customerService.updateCustomer(request);
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", response));
    }

    // === DELETE ===

    @Operation(
        summary = "Delete a customer",
        description = "Performs a soft delete of a customer. Requires version number for optimistic locking."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Customer successfully deleted",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Customer not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Optimistic locking conflict - customer was modified by another user",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Unauthorized - invalid or missing JWT token",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(
            @Parameter(description = "Customer UUID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Version number for optimistic locking", required = true)
            @RequestParam Long version) {

        DeleteCustomerRequest request = new DeleteCustomerRequest();
        request.setId(id);
        request.setVersion(version);

        customerService.deleteCustomer(request);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully", null));
    }

    // === GET BY ID ===

    @Operation(
        summary = "Get customer by ID",
        description = "Retrieves detailed information about a specific customer"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Customer found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = CustomerResponse.class)
            )
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
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(
            @Parameter(description = "Customer UUID", required = true)
            @PathVariable UUID id) {
        CustomerResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // === LIST ALL ===

    @Operation(
        summary = "List all customers",
        description = "Retrieves a list of all active (non-deleted) customers"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Customers retrieved successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = CustomerListItemResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Unauthorized - invalid or missing JWT token",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerListItemResponse>>> listAllCustomers() {
        List<CustomerListItemResponse> response = customerService.listAllCustomers();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
