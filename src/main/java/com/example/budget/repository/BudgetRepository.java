package com.example.budget.repository;

import com.example.budget.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByYearAndMonthOrderByCategoryAsc(int year, int month);

    Optional<Budget> findByCategoryAndYearAndMonth(String category, int year, int month);

    @Query("select distinct b.category from Budget b")
    List<String> findDistinctCategories();
}
