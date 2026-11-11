package com.asg.rocketflyiii.firefly;

import com.asg.rocketflyiii.config.FireflyProperties;
import com.asg.rocketflyiii.firefly.dto.FireflyAccountResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

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
    public FireflyAccountResponse getAccountsPage(int page) {
        String json = getAccounts(page);

        return new ObjectMapper().readValue(json, FireflyAccountResponse.class);
    }
}
