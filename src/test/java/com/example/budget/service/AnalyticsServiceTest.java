package com.example.budget.service;

import com.example.budget.dto.CashFlowPoint;
import com.example.budget.dto.CategorySpend;
import com.example.budget.dto.SummaryResponse;
import com.example.budget.model.TransactionType;
import com.example.budget.repository.CategoryTotal;
import com.example.budget.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 10, 1);
    private static final LocalDate TO = LocalDate.of(2026, 10, 31);

    @Mock
    private TransactionRepository transactions;
    @InjectMocks
    private AnalyticsService service;

    private static CategoryTotal total(String category, String amount) {
        return new CategoryTotal() {
            public String getCategory() { return category; }
            public BigDecimal getTotal() { return new BigDecimal(amount); }
        };
    }

    @Test
    void summaryComputesNetAndSavingsRate() {
        when(transactions.sumByTypeBetween(TransactionType.INCOME, FROM, TO)).thenReturn(new BigDecimal("1000.00"));
        when(transactions.sumByTypeBetween(TransactionType.EXPENSE, FROM, TO)).thenReturn(new BigDecimal("250.00"));

        SummaryResponse summary = service.summary(YearMonth.of(2026, 10));

        assertEquals(new BigDecimal("750.00"), summary.net());
        assertEquals(new BigDecimal("75.0"), summary.savingsRate());
    }

    @Test
    void summaryWithNoDataIsAllZero() {
        SummaryResponse summary = service.summary(YearMonth.of(2026, 10));

        assertEquals(0, summary.totalIncome().signum());
        assertEquals(0, summary.totalExpense().signum());
        assertEquals(0, summary.net().signum());
        assertEquals(0, summary.savingsRate().signum());
    }

    @Test
    void cashFlowReturnsMonthsOldestFirst() {
        when(transactions.sumByTypeBetween(eq(TransactionType.INCOME), any(), any())).thenReturn(new BigDecimal("100.00"));
        when(transactions.sumByTypeBetween(eq(TransactionType.EXPENSE), any(), any())).thenReturn(new BigDecimal("40.00"));

        List<CashFlowPoint> points = service.cashFlow(YearMonth.of(2026, 10), 3);

        assertEquals(3, points.size());
        assertEquals("Aug 2026", points.get(0).label());
        assertEquals("Oct 2026", points.get(2).label());
        assertEquals(new BigDecimal("60.00"), points.get(1).net());
    }

    @Test
    void cashFlowRejectsInvalidMonthCount() {
        assertThrows(IllegalArgumentException.class, () -> service.cashFlow(YearMonth.of(2026, 10), 0));
        assertThrows(IllegalArgumentException.class, () -> service.cashFlow(YearMonth.of(2026, 10), 25));
    }

    @Test
    void expensesByCategoryComputesPercentages() {
        when(transactions.totalsByCategory(TransactionType.EXPENSE, FROM, TO))
                .thenReturn(List.of(total("Housing", "750.00"), total("Dining", "250.00")));

        List<CategorySpend> result = service.expensesByCategory(YearMonth.of(2026, 10));

        assertEquals(new BigDecimal("75.0"), result.get(0).percentage());
        assertEquals(new BigDecimal("25.0"), result.get(1).percentage());
    }
}
