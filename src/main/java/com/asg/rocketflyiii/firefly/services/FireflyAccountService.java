package com.asg.rocketflyiii.firefly.services;

import com.asg.rocketflyiii.firefly.api.FireflyClient;
import com.asg.rocketflyiii.firefly.dto.AccountActivity;
import com.asg.rocketflyiii.firefly.dto.FireflyPage;

import org.springframework.stereotype.Service;

import tools.jackson.databind.JsonNode;

import java.time.YearMonth;
import java.util.List;

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
        return FireflyPage.collect(client::getAccountsPage);
    }

    public List<JsonNode> getAssetAccounts() {
        return getAccounts("asset");
    }

    public List<JsonNode> getAccounts(String type) {
        return FireflyPage.collect(page -> client.getAccountsPage(type, page));
    }

    public AccountActivity activity(long id, YearMonth month) {
        var account = client.getDatedAccount(id, month.atEndOfMonth());
        return AccountActivity.from(
                id,
                month,
                account,
                FireflyPage.collect(page -> client.getAccountTransactions(id, month, page)));
    }
}
