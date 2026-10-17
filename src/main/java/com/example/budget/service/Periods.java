package com.example.budget.service;

import java.time.YearMonth;

public final class Periods {
    private Periods() { }

    /** Missing values default to the current year/month; out-of-range values are rejected. */
    public static YearMonth resolve(Integer year, Integer month) {
        YearMonth now = YearMonth.now();
        int y = year == null ? now.getYear() : year;
        int m = month == null ? now.getMonthValue() : month;
        if (y < 2000 || y > 2100) {
            throw new IllegalArgumentException("Year must be between 2000 and 2100");
        }
        if (m < 1 || m > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
        return YearMonth.of(y, m);
    }
}
