package com.asg.rocketflyiii.firefly.api;

import com.asg.rocketflyiii.config.FireflyProperties;
import com.asg.rocketflyiii.firefly.dto.FireflyPage;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

@Component
public class FireflyClient {

    private final RestClient restClient;

    public FireflyClient(RestClient.Builder builder, FireflyProperties properties) {
        this.restClient =
                builder.baseUrl(properties.baseUrl() == null ? "" : properties.baseUrl())
                        .defaultHeaders(
                                headers -> {
                                    if (properties.apiToken() != null
                                            && !properties.apiToken().isBlank()) {
                                        headers.setBearerAuth(properties.apiToken());
                                    }
                                })
                        .build();
    }

    public String getAbout() {
        return restClient.get().uri(FireflyEndpoints.ABOUT).retrieve().body(String.class);
    }

    /**
     * Retrieves a page of accounts from Firefly III.
     *
     * @return the raw JSON response containing account data
     */
    public String getAccounts() {
        return restClient.get().uri(FireflyEndpoints.ACCOUNTS).retrieve().body(String.class);
    }

    /**
     * Retrieves a specific page of accounts from Firefly III.
     *
     * @param page the page number, starting at 1
     * @return the raw JSON response for the requested page
     */
    public String getAccounts(int page) {
        if (page < 1) {
            throw new IllegalArgumentException("Page number must be greater than zero");
        }

        return restClient
                .get()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path(FireflyEndpoints.ACCOUNTS)
                                        .queryParam("page", page)
                                        .build())
                .retrieve()
                .body(String.class);
    }

    /**
     * Retrieves and deserializes a specific page of Firefly III accounts.
     *
     * @param page the page number, starting at 1
     * @return the typed account response, including pagination metadata
     */
    public FireflyPage getAccountsPage(int page) {
        String json = getAccounts(page);

        return new ObjectMapper().readValue(json, FireflyPage.class);
    }

    public FireflyPage getAssetAccountsPage(int page) {
        return getAccountsPage("asset", page);
    }

    public FireflyPage getAccountsPage(String type, int page) {
        return getPage(FireflyEndpoints.ACCOUNTS, Map.of("type", type), page);
    }

    public FireflyPage getBudgetsPage(int page) {
        return getPage(FireflyEndpoints.BUDGETS, Map.of(), page);
    }

    public FireflyPage getSubscriptionsPage(int page) {
        return getPage(FireflyEndpoints.BILLS, Map.of(), page);
    }

    public FireflyPage getBudgetTransactions(long id, YearMonth month, int page) {
        return getBudgetPage(id, "transactions", month, page);
    }

    public FireflyPage getBudgetLimits(long id, YearMonth month, int page) {
        return getBudgetPage(id, "limits", month, page);
    }

    private FireflyPage getBudgetPage(long id, String resource, YearMonth month, int page) {
        return getPage(
                FireflyEndpoints.BUDGETS + "/" + id + "/" + resource,
                Map.of("start", month.atDay(1).toString(), "end", month.atEndOfMonth().toString()),
                page);
    }

    private FireflyPage getPage(String endpoint, Map<String, String> parameters, int page) {
        FireflyPage response = read(endpoint, parameters, page, FireflyPage.class);
        if (response == null
                || response.data() == null
                || response.meta() == null
                || response.meta().pagination() == null
                || response.meta().pagination().current_page() != page
                || response.meta().pagination().total_pages() < page
                || response.data().stream()
                        .anyMatch(account -> account == null || !account.isObject())) {
            throw invalidResponse();
        }
        return response;
    }

    public JsonNode getDatedAccount(long id, LocalDate date) {
        return getAttributes(FireflyEndpoints.ACCOUNTS + "/" + id, Map.of("date", date.toString()));
    }

    public JsonNode getSubscription(long id, YearMonth month) {
        return getAttributes(
                FireflyEndpoints.BILLS + "/" + id,
                Map.of("start", month.atDay(1).toString(), "end", month.atEndOfMonth().toString()));
    }

    private JsonNode getAttributes(String endpoint, Map<String, String> parameters) {
        JsonNode response = read(endpoint, parameters, 1, JsonNode.class);
        if (response == null || !response.path("data").path("attributes").isObject())
            throw invalidResponse();
        return response.path("data").path("attributes");
    }

    public FireflyPage getAccountTransactions(long id, YearMonth month, int page) {
        return getPage(
                FireflyEndpoints.ACCOUNTS + "/" + id + "/transactions",
                Map.of("start", month.atDay(1).toString(), "end", month.atEndOfMonth().toString()),
                page);
    }

    private <T> T read(String endpoint, Map<String, String> parameters, int page, Class<T> type) {
        try {
            return restClient
                    .get()
                    .uri(
                            builder -> {
                                var uri = builder.path(endpoint);
                                parameters.forEach(uri::queryParam);
                                if (page > 1) {
                                    uri.queryParam("page", page);
                                }
                                return uri.build();
                            })
                    .retrieve()
                    .body(type);
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Firefly III is unreachable");
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 401 || status == 403) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "Firefly III denied access");
            }
            throw invalidResponse();
        } catch (RestClientException ex) {
            throw invalidResponse();
        }
    }

    private static ResponseStatusException invalidResponse() {
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY, "Unexpected Firefly III response");
    }
}
