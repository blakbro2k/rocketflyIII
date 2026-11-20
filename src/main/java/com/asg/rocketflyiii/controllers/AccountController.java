package com.asg.rocketflyiii.controllers;

import com.asg.rocketflyiii.firefly.dto.AccountActivity;
import com.asg.rocketflyiii.firefly.services.FireflyAccountService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.JsonNode;

import java.time.YearMonth;
import java.util.List;
import java.util.Set;

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

    @GetMapping("/api/accounts/{id}/activity")
    public AccountActivity activity(
            @PathVariable long id,
            @RequestParam @DateTimeFormat(pattern = "uuuu-MM") YearMonth month) {
        return accounts.activity(id, month);
    }
}
