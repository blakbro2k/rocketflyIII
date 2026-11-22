package com.asg.rocketflyiii.access;

import com.asg.rocketflyiii.firefly.api.FireflyClient;
import com.asg.rocketflyiii.firefly.dto.FireflyPage;
import java.util.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

@Service
public class ResourceDiscovery {
    public record Resource(String kind, String id, String name) {}

    private final FireflyClient client;

    public ResourceDiscovery(FireflyClient client) {
        this.client = client;
    }

    public List<Resource> discover() {
        List<Resource> resources = new ArrayList<>();
        for (String type : List.of("asset", "liability"))
            add(
                    resources,
                    "account",
                    FireflyPage.collect(page -> client.getAccountsPage(type, page)));
        add(resources, "budget", FireflyPage.collect(client::getBudgetsPage));
        return resources;
    }

    private void add(List<Resource> resources, String kind, List<JsonNode> rows) {
        for (var row : rows)
            resources.add(
                    new Resource(
                            kind,
                            row.path("id").asString(),
                            row.path("attributes").path("name").asString()));
    }
}
