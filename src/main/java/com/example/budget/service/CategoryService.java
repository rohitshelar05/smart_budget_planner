package com.example.budget.service;

import com.example.budget.repository.BudgetRepository;
import com.example.budget.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
public class CategoryService {

    private static final List<String> DEFAULTS = List.of("Salary", "Freelance", "Investments", "Housing",
            "Groceries", "Dining", "Transport", "Utilities", "Health", "Entertainment", "Shopping",
            "Education", "Travel", "Savings", "Other");

    private final TransactionRepository transactions;
    private final BudgetRepository budgets;

    public CategoryService(TransactionRepository transactions, BudgetRepository budgets) {
        this.transactions = transactions;
        this.budgets = budgets;
    }

    /** Default categories plus any custom category already used, sorted case-insensitively. */
    @Transactional(readOnly = true)
    public List<String> all() {
        Set<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        set.addAll(DEFAULTS);
        set.addAll(transactions.findDistinctCategories());
        set.addAll(budgets.findDistinctCategories());
        return List.copyOf(set);
    }
}
