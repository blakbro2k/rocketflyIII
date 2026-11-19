package com.asg.rocketflyiii.firefly.services;

import com.asg.rocketflyiii.firefly.api.FireflyClient;
import com.asg.rocketflyiii.firefly.dto.FireflyAccountResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

@Service
public class FireflyAccountService {

    private final FireflyClient client;

    public FireflyAccountService(FireflyClient client) {
        this.client = client;
    }

    /**
     * Retrieves all account pages from Firefly III.
     *
     * @return accounts collected from every page
     */
    public List<JsonNode> getAllAccounts() {
        return collectPages(client::getAccountsPage);
    }

    public List<JsonNode> getAssetAccounts() {
        return getAccounts("asset");
    }

    public List<JsonNode> getAccounts(String type) {
        return collectPages(page -> client.getAccountsPage(type, page));
    }

    private List<JsonNode> collectPages(IntFunction<FireflyAccountResponse> fetchPage) {
        FireflyAccountResponse firstPage = fetchPage.apply(1);

        List<JsonNode> accounts = new ArrayList<>(firstPage.data());

        int totalPages = firstPage.meta().pagination().total_pages();

        for (int page = 2; page <= totalPages; page++) {
            FireflyAccountResponse response = fetchPage.apply(page);
            accounts.addAll(response.data());
        }

        return List.copyOf(accounts);
    }
}
