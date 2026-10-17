package com.example.budget.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record BudgetRequest(
        @NotBlank(message = "Category is required")
        @Size(max = 50, message = "Category must be at most 50 characters") String category,
        @NotNull(message = "Year is required")
        @Min(value = 2000, message = "Year must be 2000 or later")
        @Max(value = 2100, message = "Year must be 2100 or earlier") Integer year,
        @NotNull(message = "Month is required")
        @Min(value = 1, message = "Month must be between 1 and 12")
        @Max(value = 12, message = "Month must be between 1 and 12") Integer month,
        @NotNull(message = "Limit is required")
        @DecimalMin(value = "0.01", message = "Limit must be at least 0.01")
        @Digits(integer = 13, fraction = 2, message = "Limit allows up to 13 digits and 2 decimals")
        BigDecimal limitAmount) {
}
