package com.asg.rocketflyiii.firefly;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
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
