package com.asg.rocketflyiii.controllers;

import com.asg.rocketflyiii.firefly.services.FireflyAccountService;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

@RestController
public class AccountController {

    private final FireflyAccountService accounts;

    public AccountController(FireflyAccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/api/accounts")
    public List<JsonNode> getAccounts(@RequestParam(defaultValue = "asset") String type) {
        if (!Set.of("asset", "liability").contains(type)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported account type");
        }
        return accounts.getAccounts(type);
    }
}
