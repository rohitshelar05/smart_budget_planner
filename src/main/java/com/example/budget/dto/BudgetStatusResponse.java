package com.example.budget.dto;

import java.math.BigDecimal;

/** alertLevel is one of OK, WARNING (>= 80% used) or EXCEEDED (>= 100% used). */
public record BudgetStatusResponse(Long id, String category, int year, int month, BigDecimal limitAmount,
                                   BigDecimal spent, BigDecimal remaining, BigDecimal percentUsed,
                                   String alertLevel) {
}
