package com.asg.rocketflyiii.controllers;

import com.asg.rocketflyiii.firefly.api.FireflyClient;
import com.asg.rocketflyiii.firefly.dto.FireflyPage;
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
    public List<JsonNode> getSubscriptions() {
        return FireflyPage.collect(client::getSubscriptionsPage);
    }

    @GetMapping("/api/subscriptions/{id}")
    public JsonNode getSubscription(
            @PathVariable long id,
            @RequestParam @DateTimeFormat(pattern = "uuuu-MM") YearMonth month) {
        return client.getSubscription(id, month);
    }
}
