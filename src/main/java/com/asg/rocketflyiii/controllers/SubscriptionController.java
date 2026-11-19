package com.asg.rocketflyiii.controllers;

import com.asg.rocketflyiii.firefly.api.FireflyClient;
import com.asg.rocketflyiii.firefly.dto.FireflyPage;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
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
}
