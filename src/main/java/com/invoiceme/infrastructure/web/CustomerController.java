package com.invoiceme.infrastructure.web;

import com.invoiceme.application.common.ApiResponse;
import com.invoiceme.application.customer.CustomerService;
import com.invoiceme.application.customer.dto.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    // === CREATE ===

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request) {

        CustomerResponse response = customerService.createCustomer(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created successfully", response));
    }

    // === UPDATE ===

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request) {

        request.setId(id); // Ensure ID from path
        CustomerResponse response = customerService.updateCustomer(request);
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", response));
    }

    // === DELETE ===

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(
            @PathVariable UUID id,
            @RequestParam Long version) {

        DeleteCustomerRequest request = new DeleteCustomerRequest();
        request.setId(id);
        request.setVersion(version);

        customerService.deleteCustomer(request);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully", null));
    }

    // === GET BY ID ===

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomerById(@PathVariable UUID id) {
        CustomerResponse response = customerService.getCustomerById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // === LIST ALL ===

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerListItemResponse>>> listAllCustomers() {
        List<CustomerListItemResponse> response = customerService.listAllCustomers();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
