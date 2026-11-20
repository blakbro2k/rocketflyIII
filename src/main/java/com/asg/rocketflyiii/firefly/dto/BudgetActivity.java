package com.asg.rocketflyiii.firefly.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

public record BudgetActivity(List<Totals> currencies, List<Entry> transactions) {
    public record Entry(
            LocalDate date,
            String description,
            String type,
            String currency,
            BigDecimal amount,
            String source,
            String destination,
            String category,
            String runningBalance) {}

    public record Totals(
            String currency,
            BigDecimal limit,
            boolean partialPeriod,
            BigDecimal spent,
            BigDecimal transfers,
            BigDecimal remaining,
            BigDecimal remainingWithTransfers) {}

    private static class Amounts {
        BigDecimal limit;
        BigDecimal spent = BigDecimal.ZERO;
        BigDecimal transfers = BigDecimal.ZERO;
        boolean partial;

        Totals totals(String currency) {
            BigDecimal remaining = limit == null || partial ? null : limit.subtract(spent);
            return new Totals(
                    currency,
                    partial ? null : limit,
                    partial,
                    spent,
                    transfers,
                    remaining,
                    remaining == null ? null : remaining.add(transfers));
        }
    }

    public static BudgetActivity from(
            long budgetId, YearMonth month, List<JsonNode> limits, List<JsonNode> groups) {
        Map<String, Amounts> amounts = new TreeMap<>();
        List<Entry> entries = new ArrayList<>();
        for (JsonNode limit : limits) {
            JsonNode a = limit.path("attributes");
            LocalDate start = date(a, "start");
            LocalDate end = date(a, "end");
            if (end.isBefore(month.atDay(1)) || start.isAfter(month.atEndOfMonth())) continue;
            Amounts totals =
                    amounts.computeIfAbsent(field(a, "currency_code"), key -> new Amounts());
            if (start.isBefore(month.atDay(1)) || end.isAfter(month.atEndOfMonth())) {
                totals.partial = true;
            } else {
                BigDecimal value = amount(a);
                totals.limit = totals.limit == null ? value : totals.limit.add(value);
            }
        }
        for (JsonNode group : groups) {
            JsonNode splits = group.path("attributes").path("transactions");
            if (!splits.isArray()) throw invalidResponse();
            for (JsonNode split : splits) {
                JsonNode owner = split.path("budget_id");
                // A budget query may return an entire split group containing other budgets.
                if (!Long.toString(budgetId).equals(owner.asText(""))
                        && !(splits.size() == 1 && (owner.isNull() || owner.isMissingNode())))
                    continue;
                LocalDate date = date(split, "date");
                if (!YearMonth.from(date).equals(month)) continue;
                Entry entry =
                        new Entry(
                                date,
                                field(split, "description"),
                                field(split, "type"),
                                field(split, "currency_code"),
                                amount(split),
                                split.path("source_name").asText(null),
                                split.path("destination_name").asText(null),
                                split.path("category_name").asText(null),
                                split.path("running_balance").asText(null));
                entries.add(entry);
                Amounts totals = amounts.computeIfAbsent(entry.currency(), key -> new Amounts());
                if ("withdrawal".equals(entry.type()))
                    totals.spent = totals.spent.add(entry.amount());
                if ("transfer".equals(entry.type()))
                    totals.transfers = totals.transfers.add(entry.amount());
            }
        }
        entries.sort(Comparator.comparing(Entry::date).reversed());
        return new BudgetActivity(
                amounts.entrySet().stream().map(e -> e.getValue().totals(e.getKey())).toList(),
                entries);
    }

    private static String field(JsonNode node, String key) {
        String value = node.path(key).asText("");
        if (value.isBlank()) throw invalidResponse();
        return value;
    }

    private static LocalDate date(JsonNode node, String key) {
        String value = field(node, key);
        try {
            return LocalDate.parse(value.split("T", 2)[0]);
        } catch (java.time.DateTimeException e) {
            throw invalidResponse();
        }
    }

    private static BigDecimal amount(JsonNode node) {
        try {
            return new BigDecimal(field(node, "amount"));
        } catch (NumberFormatException e) {
            throw invalidResponse();
        }
    }

    private static ResponseStatusException invalidResponse() {
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY, "Unexpected Firefly III budget response");
    }
}
