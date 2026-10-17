package com.example.budget.controller;

import com.example.budget.service.CategoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MetaController {

    private final CategoryService categories;
    private final String currency;

    public MetaController(CategoryService categories, @Value("${app.currency:USD}") String currency) {
        this.categories = categories;
        this.currency = currency;
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return categories.all();
    }

    @GetMapping("/config")
    public Map<String, String> config() {
        return Map.of("currency", currency);
    }
}
