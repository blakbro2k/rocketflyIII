package com.asg.rocketflyiii.firefly;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.asg.rocketflyiii.config.FireflyProperties;
import com.asg.rocketflyiii.controllers.AccountController;
import com.asg.rocketflyiii.firefly.api.FireflyClient;
import com.asg.rocketflyiii.firefly.services.FireflyAccountService;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

class AccountControllerTest {

    private static final String BASE = "https://firefly.example.test";
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final FireflyClient client =
            new FireflyClient(builder, new FireflyProperties(BASE, "test-token"));
    private final MockMvc mvc =
            MockMvcBuilders.standaloneSetup(
                            new AccountController(new FireflyAccountService(client)))
                    .defaultRequest(
                            get("/").requestAttr(
                                            "scope",
                                            new com.asg.rocketflyiii.access.WebAccess.Scope(
                                                    true, java.util.Set.of())))
                    .build();

    @ParameterizedTest
    @ValueSource(strings = {"asset", "liability"})
    void filtersEveryPageBySelectedType(String type) throws Exception {
        String url = BASE + "/api/v1/accounts?type=" + type;
        server.expect(requestTo(url))
                .andExpect(method(HttpMethod.GET))
                .andRespond(page(1, 2, "[]"));
        server.expect(requestTo(url + "&page=2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(page(2, 2, "[]"));
        mvc.perform(get("/api/accounts").param("type", type)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"expense", "revenue", "other"})
    void rejectsUnsupportedType(String type) throws Exception {
        mvc.perform(get("/api/accounts").param("type", type)).andExpect(status().isBadRequest());
    }

    @Test
    void monthEndBalanceAndAccountSplitsDriveActivity() throws Exception {
        server.expect(requestTo(BASE + "/api/v1/accounts/7?date=2024-02-29"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(
                                """
{"data":{"attributes":{"name":"Checking","currency_code":"USD","current_balance":"110.25"}}}
""",
                                MediaType.APPLICATION_JSON));
        server.expect(
                        request ->
                                org.junit.jupiter.api.Assertions.assertEquals(
                                        "/api/v1/accounts/7/transactions",
                                        request.getURI().getPath()))
                .andExpect(method(HttpMethod.GET))
                .andExpect(
                        org.springframework.test.web.client.match.MockRestRequestMatchers
                                .queryParam("start", "2024-02-01"))
                .andExpect(
                        org.springframework.test.web.client.match.MockRestRequestMatchers
                                .queryParam("end", "2024-02-29"))
                .andRespond(
                        page(
                                1,
                                1,
                                """
[{"attributes":{"transactions":[
  {"date":"2024-02-05","type":"withdrawal","amount":"20.00","currency_code":"USD","source_id":"7","destination_id":"2"},
  {"date":"2024-02-06","type":"transfer","amount":"25","currency_code":"EUR","foreign_amount":"30.25","foreign_currency_code":"USD","source_id":"2","destination_id":"7"},
  {"date":"2024-02-06","source_id":"1","destination_id":"2"}
]}}]
"""));
        mvc.perform(get("/api/accounts/7/activity").param("month", "2024-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyBalances.length()").value(29))
                .andExpect(jsonPath("$.dailyBalances[0]").value(100.00))
                .andExpect(jsonPath("$.dailyBalances[4]").value(80.00))
                .andExpect(jsonPath("$.transactions.length()").value(2))
                .andExpect(jsonPath("$.transactions[0].amount").value(30.25))
                .andExpect(jsonPath("$.transactions[1].balance").value(80.00));
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    private void expectPage(String suffix, ResponseCreator response) {
        server.expect(requestTo(BASE + "/api/v1/accounts?type=asset" + suffix))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(response);
    }

    private ResponseCreator page(int number, int pages, String data) {
        return withSuccess(
                """
{"data":%s,"meta":{"pagination":{"total":2,"count":1,"per_page":1,"current_page":%d,"total_pages":%d}}}
"""
                        .formatted(data, number, pages),
                MediaType.APPLICATION_JSON);
    }

    @Test
    void listsEveryAssetPage() throws Exception {
        expectPage("", page(1, 2, "[{\"id\":\"1\"}]"));
        expectPage("&page=2", page(2, 2, "[{\"id\":\"2\"}]"));
        mvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(content().string("[{\"id\":\"1\"},{\"id\":\"2\"}]"));
    }

    @Test
    void returnsEmptyArray() throws Exception {
        expectPage("", page(1, 1, "[]"));
        mvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 500})
    void sanitizesUpstreamErrors(int statusCode) throws Exception {
        expectPage(
                "", withStatus(HttpStatus.valueOf(statusCode)).body("sensitive upstream response"));
        mvc.perform(get("/api/accounts"))
                .andExpect(status().isBadGateway())
                .andExpect(
                        status().reason(
                                        statusCode == 500
                                                ? "Unexpected Firefly III response"
                                                : "Firefly III denied access"))
                .andExpect(content().string(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{}", ""})
    void rejectsUnexpectedResponses(String body) throws Exception {
        expectPage("", withSuccess(body, MediaType.APPLICATION_JSON));
        mvc.perform(get("/api/accounts")).andExpect(status().isBadGateway());
    }

    @Test
    void connectionFailureOnLaterPageReturnsNoPartialResults() throws Exception {
        expectPage("", page(1, 2, "[{\"id\":\"1\"}]"));
        expectPage("&page=2", withException(new IOException("sensitive connection details")));
        mvc.perform(get("/api/accounts"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(status().reason("Firefly III is unreachable"))
                .andExpect(content().string(""));
    }
}
