package com.asg.rocketflyiii.firefly;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
public class AccountController {

    private final FireflyAccountService accounts;

    public AccountController(FireflyAccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/api/accounts")
    public List<JsonNode> getAssetAccounts() {
        return accounts.getAssetAccounts();
    }
}
