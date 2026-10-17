package com.example.budget.exception;

public class DuplicateBudgetException extends RuntimeException {
    public DuplicateBudgetException(String message) {
        super(message);
    }
}
