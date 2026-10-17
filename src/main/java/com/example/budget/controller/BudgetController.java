package com.example.budget.controller;

import com.example.budget.dto.BudgetRequest;
import com.example.budget.dto.BudgetStatusResponse;
import com.example.budget.service.BudgetService;
import com.example.budget.service.Periods;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BudgetController {

    private final BudgetService service;

    public BudgetController(BudgetService service) {
        this.service = service;
    }

    @GetMapping("/budgets")
    public List<BudgetStatusResponse> list(@RequestParam(required = false) Integer year,
                                           @RequestParam(required = false) Integer month) {
        return service.statuses(Periods.resolve(year, month));
    }

    @GetMapping("/alerts")
    public List<BudgetStatusResponse> alerts(@RequestParam(required = false) Integer year,
                                             @RequestParam(required = false) Integer month) {
        return service.alerts(Periods.resolve(year, month));
    }

    @PostMapping("/budgets")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetStatusResponse create(@Valid @RequestBody BudgetRequest request) {
        return service.create(request);
    }

    @PutMapping("/budgets/{id}")
    public BudgetStatusResponse update(@PathVariable Long id, @Valid @RequestBody BudgetRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/budgets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
