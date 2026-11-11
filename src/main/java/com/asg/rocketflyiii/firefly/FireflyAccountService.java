package com.asg.rocketflyiii.firefly;

import com.asg.rocketflyiii.firefly.dto.FireflyAccountResponse;
import java.util.ArrayList;
import java.util.List;
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
        FireflyAccountResponse firstPage = client.getAccountsPage(1);

        List<JsonNode> accounts = new ArrayList<>(firstPage.data());

        int totalPages = firstPage.meta().pagination().total_pages();

        for (int page = 2; page <= totalPages; page++) {
            FireflyAccountResponse response = client.getAccountsPage(page);
            accounts.addAll(response.data());
        }

        return List.copyOf(accounts);
    }
}
