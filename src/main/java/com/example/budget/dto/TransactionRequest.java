package com.example.budget.dto;

import com.example.budget.model.TransactionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotNull(message = "Type is required") TransactionType type,
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @Digits(integer = 13, fraction = 2, message = "Amount allows up to 13 digits and 2 decimals")
        BigDecimal amount,
        @NotBlank(message = "Category is required")
        @Size(max = 50, message = "Category must be at most 50 characters") String category,
        @Size(max = 255, message = "Description must be at most 255 characters") String description,
        @NotNull(message = "Date is required") LocalDate transactionDate) {
}
