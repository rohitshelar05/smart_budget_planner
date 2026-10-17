package com.example.budget.service;

import com.example.budget.dto.TransactionRequest;
import com.example.budget.dto.TransactionResponse;
import com.example.budget.exception.ResourceNotFoundException;
import com.example.budget.model.Transaction;
import com.example.budget.model.TransactionType;
import com.example.budget.repository.TransactionRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {

    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Direction.DESC, "transactionDate").and(Sort.by(Sort.Direction.DESC, "id"));

    private final TransactionRepository repository;

    public TransactionService(TransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> search(TransactionType type, String category, LocalDate from,
                                            LocalDate to, String search) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date must not be after 'to' date");
        }
        return repository.findAll(TransactionSpecifications.filter(type, category, from, to, search), NEWEST_FIRST)
                .stream().map(TransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(Long id) {
        return TransactionResponse.from(find(id));
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        Transaction tx = new Transaction();
        apply(tx, request);
        return TransactionResponse.from(repository.save(tx));
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request) {
        Transaction tx = find(id);
        apply(tx, request);
        return TransactionResponse.from(repository.save(tx));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw notFound(id);
        }
        repository.deleteById(id);
    }

    private Transaction find(Long id) {
        return repository.findById(id).orElseThrow(() -> notFound(id));
    }

    private ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Transaction " + id + " was not found");
    }

    private void apply(Transaction tx, TransactionRequest r) {
        tx.setType(r.type());
        tx.setAmount(r.amount());
        tx.setCategory(r.category().trim());
        String description = r.description() == null ? null : r.description().trim();
        tx.setDescription(description == null || description.isEmpty() ? null : description);
        tx.setTransactionDate(r.transactionDate());
    }
}
