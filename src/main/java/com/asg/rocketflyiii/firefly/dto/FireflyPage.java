package com.asg.rocketflyiii.firefly.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import tools.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FireflyPage(List<JsonNode> data, FireflyMeta meta) {
    public static List<JsonNode> collect(IntFunction<FireflyPage> fetchPage) {
        FireflyPage firstPage = fetchPage.apply(1);

        List<JsonNode> items = new ArrayList<>(firstPage.data());

        int totalPages = firstPage.meta().pagination().total_pages();

        for (int page = 2; page <= totalPages; page++) {
            FireflyPage response = fetchPage.apply(page);
            items.addAll(response.data());
        }

        return List.copyOf(items);
    }
}
