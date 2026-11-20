package com.asg.rocketflyiii.controllers;

import com.asg.rocketflyiii.firefly.api.FireflyClient;
import com.asg.rocketflyiii.firefly.dto.FireflyPage;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
public class SubscriptionController {
    private final FireflyClient client;

    public SubscriptionController(FireflyClient client) {
        this.client = client;
    }

    @GetMapping("/api/subscriptions")
    public List<JsonNode> getSubscriptions(
            @RequestParam(required = false) @DateTimeFormat(pattern = "uuuu-MM") YearMonth month) {
        if (month != null)
            return FireflyPage.collect(page -> client.getSubscriptionsPage(month, page));
        return FireflyPage.collect(client::getSubscriptionsPage);
    }

    @GetMapping("/api/subscriptions/{id}")
    public JsonNode getSubscription(
            @PathVariable long id,
            @RequestParam @DateTimeFormat(pattern = "uuuu-MM") YearMonth month) {
        return client.getSubscription(id, month);
    }

    public record Summary(
            JsonNode bill, BigDecimal annualAverage, BigDecimal overallAverage, List<String> rules) {}

    @GetMapping("/api/subscriptions/{id}/summary")
    public Summary summary(
            @PathVariable long id,
            @RequestParam @DateTimeFormat(pattern = "uuuu-MM") YearMonth month) {
        var bill = client.getSubscription(id, month);
        var currency = bill.path("currency_code").asString("");
        var year = month.getYear() + "-";
        BigDecimal annual = BigDecimal.ZERO;
        BigDecimal overall = BigDecimal.ZERO;
        int annualCount = 0;
        int count = 0;
        for (var group : FireflyPage.collect(page -> client.getSubscriptionTransactions(id, page))) {
            for (var split : group.path("attributes").path("transactions")) {
                if (!Long.toString(id).equals(split.path("bill_id").asString(""))) continue;
                String field;
                if (currency.equals(split.path("currency_code").asString("-"))) field = "amount";
                else if (currency.equals(split.path("foreign_currency_code").asString("-")))
                    field = "foreign_amount";
                else continue;
                if (split.path(field).isMissingNode() || split.path(field).isNull()) continue;
                var amount = new BigDecimal(split.path(field).asString()).abs().negate();
                overall = overall.add(amount);
                count++;
                if (split.path("date").asString("").startsWith(year)) {
                    annual = annual.add(amount);
                    annualCount++;
                }
            }
        }
        var rules =
                FireflyPage.collect(page -> client.getSubscriptionRules(id, page)).stream()
                        .map(rule -> rule.path("attributes").path("title").asString(""))
                        .filter(title -> !title.isBlank())
                        .toList();
        return new Summary(bill, average(annual, annualCount), average(overall, count), rules);
    }

    private BigDecimal average(BigDecimal total, int count) {
        return count == 0 ? null : total.divide(BigDecimal.valueOf(count), MathContext.DECIMAL64);
    }
}
