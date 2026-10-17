package com.example.budget.dto;

import java.math.BigDecimal;

public record CashFlowPoint(int year, int month, String label, BigDecimal income, BigDecimal expense,
                            BigDecimal net) {
}
