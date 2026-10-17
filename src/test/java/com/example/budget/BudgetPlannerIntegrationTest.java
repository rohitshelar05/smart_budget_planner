package com.example.budget;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack test against an in-memory H2 database. The properties below take precedence over any
 * local config/application.properties, so these tests never touch a developer's MySQL database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:plannertest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"})
class BudgetPlannerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private void postTransaction(String type, String amount, String category, String date) throws Exception {
        mvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"type\":\"" + type + "\",\"amount\":\"" + amount + "\",\"category\":\"" + category
                                + "\",\"description\":\"test\",\"transactionDate\":\"" + date + "\"}"))
                .andExpect(status().isCreated());
    }

    private static final String DINING_BUDGET = "{\"category\":\"Dining\",\"year\":2026,\"month\":10,\"limitAmount\":\"100.00\"}";

    @Test
    void transactionsBudgetsAndAnalyticsWorkTogether() throws Exception {
        postTransaction("INCOME", "1000.00", "Salary", "2026-10-01");
        postTransaction("EXPENSE", "85.00", "Dining", "2026-10-05");

        mvc.perform(post("/api/budgets").contentType(MediaType.APPLICATION_JSON).content(DINING_BUDGET))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alertLevel").value("WARNING"))
                .andExpect(jsonPath("$.percentUsed").value(85.0));

        mvc.perform(post("/api/budgets").contentType(MediaType.APPLICATION_JSON).content(DINING_BUDGET))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/analytics/summary").param("year", "2026").param("month", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(1000.0))
                .andExpect(jsonPath("$.totalExpense").value(85.0))
                .andExpect(jsonPath("$.net").value(915.0));

        mvc.perform(get("/api/alerts").param("year", "2026").param("month", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(get("/api/analytics/categories").param("year", "2026").param("month", "10"))
                .andExpect(jsonPath("$[0].category").value("Dining"))
                .andExpect(jsonPath("$[0].percentage").value(100.0));

        mvc.perform(get("/api/analytics/cashflow").param("year", "2026").param("month", "10").param("months", "3"))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[2].label").value("Oct 2026"));

        mvc.perform(get("/api/transactions").param("type", "EXPENSE").param("search", "din"))
                .andExpect(jsonPath("$.length()").value(1));

        mvc.perform(get("/api/categories")).andExpect(status().isOk());
        mvc.perform(get("/api/config")).andExpect(jsonPath("$.currency").exists());
    }

    @Test
    void transactionCrudLifecycle() throws Exception {
        String created = mvc.perform(post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content(
                        "{\"type\":\"EXPENSE\",\"amount\":\"20.00\",\"category\":\"Transport\","
                                + "\"transactionDate\":\"2026-10-02\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = created.replaceAll(".*\"id\":(\\d+).*", "$1");

        mvc.perform(put("/api/transactions/" + id).contentType(MediaType.APPLICATION_JSON).content(
                        "{\"type\":\"EXPENSE\",\"amount\":\"25.50\",\"category\":\"Transport\","
                                + "\"transactionDate\":\"2026-10-02\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(25.5));

        mvc.perform(delete("/api/transactions/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/transactions/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void invalidPeriodReturns400() throws Exception {
        mvc.perform(get("/api/analytics/summary").param("year", "2026").param("month", "13"))
                .andExpect(status().isBadRequest());
    }
}
