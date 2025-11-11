package com.invoiceme.application.seeddata;

import java.util.ArrayList;
import java.util.List;

/**
 * Result object for seed data generation operations.
 */
public class SeedDataResult {

    private int successCount;
    private int errorCount;
    private List<String> errors;

    public SeedDataResult() {
        this.errors = new ArrayList<>();
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public int getTotalCount() {
        return successCount + errorCount;
    }

    public boolean hasErrors() {
        return errorCount > 0;
    }

    @Override
    public String toString() {
        return "SeedDataResult{" +
                "successCount=" + successCount +
                ", errorCount=" + errorCount +
                ", errors=" + errors +
                '}';
    }
}
