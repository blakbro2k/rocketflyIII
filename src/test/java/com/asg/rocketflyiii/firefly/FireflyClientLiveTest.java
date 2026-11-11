package com.asg.rocketflyiii.firefly;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "FIREFLY_LIVE_TEST", matches = "true")
class FireflyClientLiveTest {

    @Autowired private FireflyClient client;
    @Autowired private FireflyAccountService accountService;

    @Test
    void connectsToFirefly() {
        String response = client.getAbout();

        assertNotNull(response);
        assertTrue(response.contains("\"version\""));
    }

    @Test
    void inspectAccountsResponse() throws Exception {
        String response = client.getAccounts(1);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);

        assertTrue(root.has("data"));
        assertTrue(root.has("meta"));

        System.out.println(
                mapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(root.path("meta").path("pagination")));
    }

    @Test
    void retrievesAllAccounts() {
        List<JsonNode> accounts = accountService.getAllAccounts();

        assertNotNull(accounts);
        assertFalse(accounts.isEmpty());

        System.out.println("Retrieved accounts: " + accounts.size());
    }
}
