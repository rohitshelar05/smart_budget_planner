package com.example.budget.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class Money {
    private Money() { }

    static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value;
    }

    /** part / whole * 100 with one decimal; zero when whole is not positive. */
    static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() <= 0) {
            return BigDecimal.ZERO.setScale(1);
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 1, RoundingMode.HALF_UP);
    }
}
