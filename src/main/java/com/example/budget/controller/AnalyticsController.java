package com.example.budget.controller;

import com.example.budget.dto.CashFlowPoint;
import com.example.budget.dto.CategorySpend;
import com.example.budget.dto.SummaryResponse;
import com.example.budget.service.AnalyticsService;
import com.example.budget.service.Periods;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public SummaryResponse summary(@RequestParam(required = false) Integer year,
                                   @RequestParam(required = false) Integer month) {
        return service.summary(Periods.resolve(year, month));
    }

    @GetMapping("/cashflow")
    public List<CashFlowPoint> cashFlow(@RequestParam(required = false) Integer year,
                                        @RequestParam(required = false) Integer month,
                                        @RequestParam(defaultValue = "6") int months) {
        return service.cashFlow(Periods.resolve(year, month), months);
    }

    @GetMapping("/categories")
    public List<CategorySpend> categories(@RequestParam(required = false) Integer year,
                                          @RequestParam(required = false) Integer month) {
        return service.expensesByCategory(Periods.resolve(year, month));
    }
}
