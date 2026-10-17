package com.example.budget.service;

import com.example.budget.dto.BudgetRequest;
import com.example.budget.dto.BudgetStatusResponse;
import com.example.budget.exception.DuplicateBudgetException;
import com.example.budget.exception.ResourceNotFoundException;
import com.example.budget.model.Budget;
import com.example.budget.model.TransactionType;
import com.example.budget.repository.BudgetRepository;
import com.example.budget.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 10, 1);
    private static final LocalDate TO = LocalDate.of(2026, 10, 31);

    @Mock
    private BudgetRepository budgets;
    @Mock
    private TransactionRepository transactions;
    @InjectMocks
    private BudgetService service;

    private Budget budget(Long id, String category, String limit) {
        Budget b = new Budget();
        b.setId(id);
        b.setCategory(category);
        b.setYear(2026);
        b.setMonth(10);
        b.setLimitAmount(new BigDecimal(limit));
        return b;
    }

    private BudgetRequest request(String category, String limit) {
        return new BudgetRequest(category, 2026, 10, new BigDecimal(limit));
    }

    @ParameterizedTest
    @CsvSource({
            "0.00,OK", "50.00,OK", "79.99,OK",
            "80.00,WARNING", "99.99,WARNING",
            "100.00,EXCEEDED", "150.00,EXCEEDED"})
    void alertLevelFollowsThresholds(String spent, String expectedLevel) {
        when(budgets.findByYearAndMonthOrderByCategoryAsc(2026, 10))
                .thenReturn(List.of(budget(1L, "Dining", "100.00")));
        when(transactions.sumByTypeAndCategoryBetween(TransactionType.EXPENSE, "Dining", FROM, TO))
                .thenReturn(new BigDecimal(spent));

        BudgetStatusResponse status = service.statuses(YearMonth.of(2026, 10)).get(0);

        assertEquals(expectedLevel, status.alertLevel());
        assertEquals(0, new BigDecimal("100.00").subtract(new BigDecimal(spent)).compareTo(status.remaining()));
    }

    @Test
    void percentUsedIsRoundedToOneDecimal() {
        when(budgets.findByYearAndMonthOrderByCategoryAsc(2026, 10))
                .thenReturn(List.of(budget(1L, "Dining", "300.00")));
        when(transactions.sumByTypeAndCategoryBetween(TransactionType.EXPENSE, "Dining", FROM, TO))
                .thenReturn(new BigDecimal("100.00"));

        assertEquals(new BigDecimal("33.3"), service.statuses(YearMonth.of(2026, 10)).get(0).percentUsed());
    }

    @Test
    void alertsContainOnlyBudgetsAtOrAboveEightyPercent() {
        when(budgets.findByYearAndMonthOrderByCategoryAsc(2026, 10)).thenReturn(List.of(
                budget(1L, "Dining", "100.00"), budget(2L, "Travel", "100.00")));
        when(transactions.sumByTypeAndCategoryBetween(TransactionType.EXPENSE, "Dining", FROM, TO))
                .thenReturn(new BigDecimal("85.00"));
        when(transactions.sumByTypeAndCategoryBetween(TransactionType.EXPENSE, "Travel", FROM, TO))
                .thenReturn(new BigDecimal("10.00"));

        List<BudgetStatusResponse> alerts = service.alerts(YearMonth.of(2026, 10));

        assertEquals(1, alerts.size());
        assertEquals("Dining", alerts.get(0).category());
    }

    @Test
    void createRejectsDuplicateCategoryAndMonth() {
        when(budgets.findByCategoryAndYearAndMonth("Dining", 2026, 10))
                .thenReturn(Optional.of(budget(1L, "Dining", "100.00")));

        assertThrows(DuplicateBudgetException.class, () -> service.create(request(" Dining ", "200.00")));
        verify(budgets, never()).save(any());
    }

    @Test
    void createPersistsNewBudgetWithZeroSpent() {
        when(budgets.findByCategoryAndYearAndMonth("Dining", 2026, 10)).thenReturn(Optional.empty());
        when(budgets.save(any(Budget.class))).thenAnswer(inv -> {
            Budget b = inv.getArgument(0);
            b.setId(4L);
            return b;
        });

        BudgetStatusResponse status = service.create(request("Dining", "200.00"));

        assertEquals(4L, status.id());
        assertEquals(0, BigDecimal.ZERO.compareTo(status.spent()));
        assertEquals("OK", status.alertLevel());
    }

    @Test
    void updateRejectsPeriodUsedByAnotherBudget() {
        when(budgets.findById(1L)).thenReturn(Optional.of(budget(1L, "Dining", "100.00")));
        when(budgets.findByCategoryAndYearAndMonth("Travel", 2026, 10))
                .thenReturn(Optional.of(budget(2L, "Travel", "500.00")));

        assertThrows(DuplicateBudgetException.class, () -> service.update(1L, request("Travel", "100.00")));
        verify(budgets, never()).save(any());
    }

    @Test
    void updateAllowsKeepingTheSameCategoryAndPeriod() {
        Budget existing = budget(1L, "Dining", "100.00");
        when(budgets.findById(1L)).thenReturn(Optional.of(existing));
        when(budgets.findByCategoryAndYearAndMonth("Dining", 2026, 10)).thenReturn(Optional.of(existing));
        when(budgets.save(existing)).thenReturn(existing);

        BudgetStatusResponse status = service.update(1L, request("Dining", "250.00"));

        assertEquals(new BigDecimal("250.00"), status.limitAmount());
    }

    @Test
    void deleteMissingBudgetThrowsNotFound() {
        when(budgets.existsById(9L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.delete(9L));
        verify(budgets, never()).deleteById(any());
    }
}
