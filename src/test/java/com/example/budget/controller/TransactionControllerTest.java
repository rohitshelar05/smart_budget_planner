package com.example.budget.controller;

import com.example.budget.dto.TransactionResponse;
import com.example.budget.exception.ResourceNotFoundException;
import com.example.budget.model.TransactionType;
import com.example.budget.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private TransactionService service;

    @Test
    void postValidTransactionReturns201() throws Exception {
        when(service.create(any())).thenReturn(new TransactionResponse(1L, TransactionType.EXPENSE,
                new BigDecimal("12.50"), "Dining", "Lunch", LocalDate.of(2026, 10, 5)));

        mvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content("""
                        {"type":"EXPENSE","amount":"12.50","category":"Dining","description":"Lunch",
                         "transactionDate":"2026-10-05"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.transactionDate").value("2026-10-05"));
    }

    @Test
    void postInvalidTransactionReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content("""
                        {"type":"EXPENSE","amount":0,"category":"  ","transactionDate":"2026-10-05"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists())
                .andExpect(jsonPath("$.fieldErrors.category").exists());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void getUnknownTransactionReturns404() throws Exception {
        when(service.get(42L)).thenThrow(new ResourceNotFoundException("Transaction 42 was not found"));

        mvc.perform(get("/api/transactions/42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transaction 42 was not found"));
    }

    @Test
    void invalidTypeFilterReturns400() throws Exception {
        mvc.perform(get("/api/transactions").param("type", "BOGUS")).andExpect(status().isBadRequest());
    }

    @Test
    void invalidDateFilterReturns400() throws Exception {
        mvc.perform(get("/api/transactions").param("from", "yesterday")).andExpect(status().isBadRequest());
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/transactions/5")).andExpect(status().isNoContent());
        verify(service).delete(5L);
    }
}
