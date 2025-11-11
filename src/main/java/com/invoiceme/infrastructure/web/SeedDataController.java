package com.invoiceme.infrastructure.web;

import com.invoiceme.application.common.ApiResponse;
import com.invoiceme.application.seeddata.SeedDataResult;
import com.invoiceme.application.seeddata.SeedDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for seed data generation (testing and demo purposes).
 * This endpoint is protected by authentication and should be used with caution.
 */
@RestController
@RequestMapping("/api/seed-data")
@Tag(name = "Seed Data", description = "Generate test data for demos and testing")
@SecurityRequirement(name = "bearerAuth")
public class SeedDataController {

    private static final Logger logger = LoggerFactory.getLogger(SeedDataController.class);

    @Autowired
    private SeedDataService seedDataService;

    @Operation(
        summary = "Generate seed data",
        description = "Generates a batch of test customers with invoices, line items, and payments. " +
                     "Data is created in batches of 10. Each customer gets 5-10 paid invoices, " +
                     "0-2 sent invoices, and 0-2 draft invoices. All invoices have 1-5 line items, " +
                     "and paid invoices have 1-3 payments. Data spans the past 6 months. " +
                     "WARNING: This creates real data in your database!"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Seed data generated successfully (may include partial failures)",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = SeedDataResult.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid batch size (must be 1-100)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Unauthorized - invalid or missing JWT token",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "Server error during data generation",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
        )
    })
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<SeedDataResult>> generateSeedData(
            @Parameter(description = "Number of customers to create (default: 10, max: 100)")
            @RequestParam(defaultValue = "10") int count) {

        logger.info("Seed data generation requested: count={}", count);

        // Validate count
        if (count < 1 || count > 100) {
            logger.warn("Invalid seed data count requested: {}", count);
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.failure("Count must be between 1 and 100"));
        }

        try {
            SeedDataResult result = seedDataService.generateBatch(count);

            String message = String.format("Seed data generation completed: %d succeeded, %d failed",
                result.getSuccessCount(), result.getErrorCount());

            logger.info(message);

            // Return 200 even if some failed, client can check result for details
            return ResponseEntity
                    .ok(ApiResponse.success(message, result));

        } catch (Exception e) {
            logger.error("Unexpected error during seed data generation", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("Seed data generation failed: " + e.getMessage()));
        }
    }
}
