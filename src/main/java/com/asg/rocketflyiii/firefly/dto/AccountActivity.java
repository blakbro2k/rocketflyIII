package com.asg.rocketflyiii.firefly.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

public record AccountActivity(
        String name,
        String currency,
        BigDecimal closingBalance,
        List<BigDecimal> dailyBalances,
        List<Entry> transactions) {
    public record Entry(
            String date,
            String description,
            String type,
            String source,
            String destination,
            String category,
            String budget,
            BigDecimal amount,
            BigDecimal balance) {}

    public static AccountActivity from(
            long id, YearMonth month, JsonNode account, List<JsonNode> groups) {
        String currency = account.path("currency_code").asText("");
        String owner = Long.toString(id);
        List<JsonNode> splits = new ArrayList<>();
        for (var group : groups) {
            var transactions = group.path("attributes").path("transactions");
            if (!transactions.isArray()) throw invalid();
            for (var split : transactions) {
                if (owner.equals(text(split, "source_id"))
                        || owner.equals(text(split, "destination_id"))) splits.add(split);
            }
        }
        splits.sort(
                Comparator.comparing((JsonNode split) -> text(split, "date"))
                        .reversed()
                        .thenComparing(
                                split -> split.path("transaction_journal_id").asLong(0),
                                Comparator.reverseOrder()));
        BigDecimal closing = decimal(account, "current_balance");
        BigDecimal balance = closing;
        List<Entry> entries = new ArrayList<>();
        BigDecimal[] days = new BigDecimal[month.lengthOfMonth()];
        int cursor = days.length;
        for (var split : splits) {
            String timestamp = text(split, "date");
            LocalDate date;
            try {
                date = LocalDate.parse(timestamp.split("T", 2)[0]);
            } catch (java.time.DateTimeException ex) {
                throw invalid();
            }
            if (!YearMonth.from(date).equals(month)) continue;
            while (cursor >= date.getDayOfMonth()) days[--cursor] = balance;
            String field =
                    currency.equals(text(split, "currency_code")) ? "amount" : "foreign_amount";
            if (field.equals("foreign_amount")
                    && !currency.equals(text(split, "foreign_currency_code"))) throw invalid();
            BigDecimal amount = decimal(split, field);
            if (owner.equals(text(split, "source_id"))) amount = amount.negate();
            if (owner.equals(text(split, "source_id"))
                    && owner.equals(text(split, "destination_id"))) amount = BigDecimal.ZERO;
            entries.add(
                    new Entry(
                            date.toString(),
                            text(split, "description"),
                            text(split, "type"),
                            text(split, "source_name"),
                            text(split, "destination_name"),
                            text(split, "category_name"),
                            text(split, "budget_name"),
                            amount,
                            balance));
            balance = balance.subtract(amount);
        }
        while (cursor > 0) days[--cursor] = balance;
        return new AccountActivity(
                text(account, "name"), currency, closing, List.of(days), entries);
    }

    private static String text(JsonNode node, String key) {
        return node.path(key).asText("");
    }

    private static BigDecimal decimal(JsonNode node, String key) {
        try {
            return new BigDecimal(text(node, key));
        } catch (NumberFormatException ex) {
            throw invalid();
        }
    }

    private static ResponseStatusException invalid() {
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY, "Unexpected Firefly III account activity");
    }
}
