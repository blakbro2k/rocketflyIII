package com.asg.rocketflyiii.controllers;

import com.asg.rocketflyiii.access.WebAccess.Scope;
import com.asg.rocketflyiii.firefly.api.FireflyClient;
import com.asg.rocketflyiii.firefly.dto.BudgetActivity;
import com.asg.rocketflyiii.firefly.dto.FireflyPage;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
public class BudgetController {
    private final FireflyClient client;

    public BudgetController(FireflyClient client) {
        this.client = client;
    }

    @GetMapping("/api/budgets")
    public List<JsonNode> getBudgets(@RequestAttribute Scope scope) {
        return scope.visible("budget", FireflyPage.collect(client::getBudgetsPage));
    }

    @GetMapping("/api/budgets/{id}/activity")
    public BudgetActivity getActivity(
            @PathVariable long id,
            @RequestParam @DateTimeFormat(pattern = "uuuu-MM") YearMonth month) {
        return BudgetActivity.from(
                id,
                month,
                FireflyPage.collect(page -> client.getBudgetLimits(id, month, page)),
                FireflyPage.collect(page -> client.getBudgetTransactions(id, month, page)));
    }
}
