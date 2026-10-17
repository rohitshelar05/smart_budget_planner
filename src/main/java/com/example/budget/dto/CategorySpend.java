package com.example.budget.dto;

import java.math.BigDecimal;

public record CategorySpend(String category, BigDecimal total, BigDecimal percentage) {
}
