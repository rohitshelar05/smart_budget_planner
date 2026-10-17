package com.example.budget.service;

import com.example.budget.dto.TransactionRequest;
import com.example.budget.dto.TransactionResponse;
import com.example.budget.exception.ResourceNotFoundException;
import com.example.budget.model.Transaction;
import com.example.budget.model.TransactionType;
import com.example.budget.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;

    @InjectMocks
    private TransactionService service;

    private TransactionRequest request(String category, String description) {
        return new TransactionRequest(TransactionType.EXPENSE, new BigDecimal("12.50"), category, description,
                LocalDate.of(2026, 10, 5));
    }

    @Test
    void createTrimsTextAndPersists() {
        when(repository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(7L);
            return t;
        });

        TransactionResponse response = service.create(request("  Dining  ", "   "));

        assertEquals(7L, response.id());
        assertEquals("Dining", response.category());
        assertNull(response.description());
        assertEquals(new BigDecimal("12.50"), response.amount());
        assertEquals(LocalDate.of(2026, 10, 5), response.transactionDate());
    }

    @Test
    void getMissingTransactionThrowsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.get(99L));
    }

    @Test
    void updateChangesAllFields() {
        Transaction existing = new Transaction();
        existing.setId(3L);
        existing.setType(TransactionType.INCOME);
        existing.setAmount(BigDecimal.ONE);
        existing.setCategory("Old");
        when(repository.findById(3L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        TransactionResponse response = service.update(3L, request("Groceries", "Weekly shop"));

        assertEquals(TransactionType.EXPENSE, response.type());
        assertEquals("Groceries", response.category());
        assertEquals("Weekly shop", response.description());
    }

    @Test
    void deleteMissingTransactionThrowsAndDeletesNothing() {
        when(repository.existsById(5L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.delete(5L));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteExistingTransactionDeletesIt() {
        when(repository.existsById(5L)).thenReturn(true);
        service.delete(5L);
        verify(repository).deleteById(5L);
    }

    @Test
    void searchRejectsReversedDateRange() {
        assertThrows(IllegalArgumentException.class, () -> service.search(null, null,
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1), null));
    }
}
