package com.example.budget.repository;

import com.example.budget.model.Transaction;
import com.example.budget.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    /** Returns null when there are no matching rows. */
    @Query("select sum(t.amount) from Transaction t where t.type = :type "
            + "and t.transactionDate between :from and :to")
    BigDecimal sumByTypeBetween(@Param("type") TransactionType type,
                                @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Returns null when there are no matching rows. */
    @Query("select sum(t.amount) from Transaction t where t.type = :type and t.category = :category "
            + "and t.transactionDate between :from and :to")
    BigDecimal sumByTypeAndCategoryBetween(@Param("type") TransactionType type, @Param("category") String category,
                                           @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select t.category as category, sum(t.amount) as total from Transaction t "
            + "where t.type = :type and t.transactionDate between :from and :to "
            + "group by t.category order by sum(t.amount) desc")
    List<CategoryTotal> totalsByCategory(@Param("type") TransactionType type,
                                         @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select distinct t.category from Transaction t")
    List<String> findDistinctCategories();
}
