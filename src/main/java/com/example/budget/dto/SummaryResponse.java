package com.example.budget.dto;

import java.math.BigDecimal;

public record SummaryResponse(int year, int month, BigDecimal totalIncome, BigDecimal totalExpense,
                              BigDecimal net, BigDecimal savingsRate) {
}
