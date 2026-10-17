package com.example.budget.service;

import com.example.budget.dto.BudgetRequest;
import com.example.budget.dto.BudgetStatusResponse;
import com.example.budget.exception.DuplicateBudgetException;
import com.example.budget.exception.ResourceNotFoundException;
import com.example.budget.model.Budget;
import com.example.budget.model.TransactionType;
import com.example.budget.repository.BudgetRepository;
import com.example.budget.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class BudgetService {

    static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    static final BigDecimal EIGHTY = BigDecimal.valueOf(80);

    private final BudgetRepository budgets;
    private final TransactionRepository transactions;

    public BudgetService(BudgetRepository budgets, TransactionRepository transactions) {
        this.budgets = budgets;
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    public List<BudgetStatusResponse> statuses(YearMonth period) {
        return budgets.findByYearAndMonthOrderByCategoryAsc(period.getYear(), period.getMonthValue())
                .stream().map(this::toStatus).toList();
    }

    /** Budgets that have reached 80% or more of their limit. */
    @Transactional(readOnly = true)
    public List<BudgetStatusResponse> alerts(YearMonth period) {
        return statuses(period).stream().filter(s -> !"OK".equals(s.alertLevel())).toList();
    }

    @Transactional
    public BudgetStatusResponse create(BudgetRequest request) {
        String category = request.category().trim();
        budgets.findByCategoryAndYearAndMonth(category, request.year(), request.month()).ifPresent(b -> {
            throw duplicate(category, request.year(), request.month());
        });
        Budget budget = new Budget();
        apply(budget, request, category);
        return toStatus(budgets.save(budget));
    }

    @Transactional
    public BudgetStatusResponse update(Long id, BudgetRequest request) {
        Budget budget = budgets.findById(id).orElseThrow(() -> notFound(id));
        String category = request.category().trim();
        budgets.findByCategoryAndYearAndMonth(category, request.year(), request.month()).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw duplicate(category, request.year(), request.month());
            }
        });
        apply(budget, request, category);
        return toStatus(budgets.save(budget));
    }

    @Transactional
    public void delete(Long id) {
        if (!budgets.existsById(id)) {
            throw notFound(id);
        }
        budgets.deleteById(id);
    }

    private void apply(Budget budget, BudgetRequest r, String category) {
        budget.setCategory(category);
        budget.setYear(r.year());
        budget.setMonth(r.month());
        budget.setLimitAmount(r.limitAmount());
    }

    private BudgetStatusResponse toStatus(Budget b) {
        YearMonth ym = YearMonth.of(b.getYear(), b.getMonth());
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        BigDecimal spent = Money.nz(
                transactions.sumByTypeAndCategoryBetween(TransactionType.EXPENSE, b.getCategory(), from, to));
        return buildStatus(b, spent);
    }

    static BudgetStatusResponse buildStatus(Budget b, BigDecimal spent) {
        BigDecimal limit = b.getLimitAmount();
        String level = "OK";
        if (spent.compareTo(limit) >= 0) {
            level = "EXCEEDED";
        } else if (spent.multiply(HUNDRED).compareTo(limit.multiply(EIGHTY)) >= 0) {
            level = "WARNING";
        }
        return new BudgetStatusResponse(b.getId(), b.getCategory(), b.getYear(), b.getMonth(), limit, spent,
                limit.subtract(spent), Money.percent(spent, limit), level);
    }

    private ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Budget " + id + " was not found");
    }

    private DuplicateBudgetException duplicate(String category, int year, int month) {
        return new DuplicateBudgetException(
                "A budget for '" + category + "' already exists for " + YearMonth.of(year, month));
    }
}
