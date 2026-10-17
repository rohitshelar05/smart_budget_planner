package com.example.budget.service;

import com.example.budget.dto.CashFlowPoint;
import com.example.budget.dto.CategorySpend;
import com.example.budget.dto.SummaryResponse;
import com.example.budget.model.TransactionType;
import com.example.budget.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AnalyticsService {

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final TransactionRepository transactions;

    public AnalyticsService(TransactionRepository transactions) {
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    public SummaryResponse summary(YearMonth period) {
        BigDecimal income = total(TransactionType.INCOME, period);
        BigDecimal expense = total(TransactionType.EXPENSE, period);
        BigDecimal net = income.subtract(expense);
        BigDecimal savingsRate = income.signum() > 0 ? Money.percent(net, income) : BigDecimal.ZERO.setScale(1);
        return new SummaryResponse(period.getYear(), period.getMonthValue(), income, expense, net, savingsRate);
    }

    /** Income vs expense for the {@code months} months ending at {@code end}, oldest first. */
    @Transactional(readOnly = true)
    public List<CashFlowPoint> cashFlow(YearMonth end, int months) {
        if (months < 1 || months > 24) {
            throw new IllegalArgumentException("Months must be between 1 and 24");
        }
        List<CashFlowPoint> points = new ArrayList<>();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = end.minusMonths(i);
            BigDecimal income = total(TransactionType.INCOME, ym);
            BigDecimal expense = total(TransactionType.EXPENSE, ym);
            points.add(new CashFlowPoint(ym.getYear(), ym.getMonthValue(), ym.format(LABEL),
                    income, expense, income.subtract(expense)));
        }
        return points;
    }

    @Transactional(readOnly = true)
    public List<CategorySpend> expensesByCategory(YearMonth period) {
        var rows = transactions.totalsByCategory(TransactionType.EXPENSE, period.atDay(1), period.atEndOfMonth());
        BigDecimal all = rows.stream().map(r -> Money.nz(r.getTotal())).reduce(BigDecimal.ZERO, BigDecimal::add);
        return rows.stream()
                .map(r -> new CategorySpend(r.getCategory(), r.getTotal(), Money.percent(r.getTotal(), all)))
                .toList();
    }

    private BigDecimal total(TransactionType type, YearMonth ym) {
        return Money.nz(transactions.sumByTypeBetween(type, ym.atDay(1), ym.atEndOfMonth()));
    }
}
