package com.example.budget.dto;

import com.example.budget.model.Transaction;
import com.example.budget.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(Long id, TransactionType type, BigDecimal amount, String category,
                                  String description, LocalDate transactionDate) {

    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(t.getId(), t.getType(), t.getAmount(), t.getCategory(),
                t.getDescription(), t.getTransactionDate());
    }
}
