package com.asg.rocketflyiii.controllers;

import com.asg.rocketflyiii.firefly.api.FireflyClient;
import com.asg.rocketflyiii.firefly.dto.FireflyPage;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
public class BudgetController {
    private final FireflyClient client;

    public BudgetController(FireflyClient client) {
        this.client = client;
    }

    @GetMapping("/api/budgets")
    public List<JsonNode> getBudgets() {
        return FireflyPage.collect(client::getBudgetsPage);
    }
}
