package com.asg.rocketflyiii.firefly;

import com.asg.rocketflyiii.config.FireflyProperties;
import com.asg.rocketflyiii.firefly.dto.FireflyAccountResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
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

    FireflyAccountResponse getAssetAccountsPage(int page) {
        try {
            FireflyAccountResponse response =
                    restClient
                            .get()
                            .uri(
                                    builder -> {
                                        var uri =
                                                builder.path(FireflyEndpoints.ACCOUNTS)
                                                        .queryParam("type", "asset");
                                        if (page > 1) {
                                            uri.queryParam("page", page);
                                        }
                                        return uri.build();
                                    })
                            .retrieve()
                            .body(FireflyAccountResponse.class);
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
