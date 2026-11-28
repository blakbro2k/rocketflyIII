package com.asg.rocketflyiii.firefly;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.asg.rocketflyiii.config.FireflyProperties;
import com.asg.rocketflyiii.controllers.BudgetController;
import com.asg.rocketflyiii.firefly.api.FireflyClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

class BudgetControllerTest {
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final MockMvc mvc =
            MockMvcBuilders.standaloneSetup(
                            new BudgetController(
                                    new FireflyClient(
                                            builder,
                                            new FireflyProperties(
                                                    "https://firefly.test", "test-token"))))
                    .defaultRequest(
                            get("/").requestAttr(
                                            "scope",
                                            new com.asg.rocketflyiii.access.WebAccess.Scope(
                                                    true, java.util.Set.of())))
                    .build();

    private void expectPage(String query, ResponseCreator response) {
        server.expect(requestTo("https://firefly.test/api/v1/budgets" + query))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(response);
    }

    private ResponseCreator page(String data, int current, int last) {
        return withSuccess(
                """
                {"data":%s,"meta":{"pagination":{"total":2,"count":1,"per_page":1,"current_page":%d,"total_pages":%d}}}
                """
                        .formatted(data, current, last),
                MediaType.APPLICATION_JSON);
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void returnsActiveAndInactiveBudgetsAcrossPages() throws Exception {
        expectPage("", page("[{\"attributes\":{\"name\":\"Food\",\"active\":true}}]", 1, 2));
        expectPage(
                "?page=2", page("[{\"attributes\":{\"name\":\"Travel\",\"active\":false}}]", 2, 2));
        mvc.perform(get("/api/budgets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].attributes.name").value("Food"))
                .andExpect(jsonPath("$[1].attributes.active").value(false));
    }

    @Test
    void reportsScopedMonthlyActivityAcrossPages() throws Exception {
        String base = "https://firefly.test/api/v1/budgets/7/";
        server.expect(
                        request ->
                                org.junit.jupiter.api.Assertions.assertEquals(
                                        base + "limits",
                                        request.getURI().toString().split("[?]")[0]))
                .andExpect(queryParam("start", "2026-11-01"))
                .andExpect(queryParam("end", "2026-11-30"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        page(
                                """
                [{"attributes":{"amount":"100.00","currency_code":"USD","start":"2026-11-01","end":"2026-11-30"}}]
                """,
                                1,
                                1));
        String group =
                """
                [{"attributes":{"transactions":[
                {"budget_id":"7","type":"withdrawal","description":"Food","date":"2026-11-02","currency_code":"USD","amount":"75.15"},
                {"budget_id":"8","type":"withdrawal","description":"Other","date":"2026-11-02","currency_code":"USD","amount":"20"}
                ]}}]
                """;
        server.expect(
                        request ->
                                org.junit.jupiter.api.Assertions.assertEquals(
                                        "/api/v1/budgets/7/transactions",
                                        request.getURI().getPath()))
                .andExpect(queryParam("start", "2026-11-01"))
                .andRespond(page(group, 1, 2));
        server.expect(queryParam("page", "2"))
                .andExpect(queryParam("end", "2026-11-30"))
                .andRespond(
                        page(
                                """
                [{"attributes":{"transactions":[{"budget_id":"7","type":"transfer","description":"Funding","date":"2026-11-10","currency_code":"USD","amount":"10.25"}]}}]
                """,
                                2,
                                2));
        mvc.perform(get("/api/budgets/7/activity").param("month", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions.length()").value(2))
                .andExpect(jsonPath("$.transactions[0].type").value("transfer"))
                .andExpect(jsonPath("$.currencies[0].remaining").value(24.85))
                .andExpect(jsonPath("$.currencies[0].remainingWithTransfers").value(35.10));
    }

    @Test
    void handlesEmptyList() throws Exception {
        expectPage("", page("[]", 1, 1));
        mvc.perform(get("/api/budgets"))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
    }

    @Test
    void handlesFailureOnLaterPage() throws Exception {
        expectPage("", page("[]", 1, 2));
        expectPage("?page=2", withStatus(HttpStatus.FORBIDDEN).body("sensitive upstream response"));
        mvc.perform(get("/api/budgets"))
                .andExpect(status().isBadGateway())
                .andExpect(content().string(""));
    }
}
