package com.asg.rocketflyiii.firefly;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.asg.rocketflyiii.config.FireflyProperties;
import com.asg.rocketflyiii.controllers.SubscriptionController;
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

class SubscriptionControllerTest {
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final MockMvc mvc =
            MockMvcBuilders.standaloneSetup(
                            new SubscriptionController(
                                    new FireflyClient(
                                            builder,
                                            new FireflyProperties(
                                                    "https://firefly.test", "test-token"))))
                    .build();

    private void expectPage(String query, ResponseCreator response) {
        server.expect(requestTo("https://firefly.test/api/v1/bills" + query))
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

    @Test
    void requestsSubscriptionForSelectedMonth() throws Exception {
        server.expect(requestTo(startsWith("https://firefly.test/api/v1/bills/7?")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("start", "2024-02-01"))
                .andExpect(queryParam("end", "2024-02-29"))
                .andRespond(
                        withSuccess(
                                "{\"data\":{\"attributes\":{\"name\":\"Music\",\"paid_dates\":[]}}}",
                                MediaType.APPLICATION_JSON));
        mvc.perform(get("/api/subscriptions/7").param("month", "2024-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Music"))
                .andExpect(jsonPath("$.paid_dates").isArray());
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void returnsActiveAndInactiveSubscriptionsAcrossPages() throws Exception {
        expectPage("", page("[{\"attributes\":{\"name\":\"Internet\",\"active\":true}}]", 1, 2));
        expectPage(
                "?page=2", page("[{\"attributes\":{\"name\":\"Music\",\"active\":false}}]", 2, 2));
        mvc.perform(get("/api/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].attributes.name").value("Internet"))
                .andExpect(jsonPath("$[1].attributes.active").value(false));
    }

    @Test
    void handlesEmptyList() throws Exception {
        expectPage("", page("[]", 1, 1));
        mvc.perform(get("/api/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
    }

    @Test
    void handlesFailureOnLaterPage() throws Exception {
        expectPage("", page("[]", 1, 2));
        expectPage("?page=2", withStatus(HttpStatus.FORBIDDEN).body("sensitive upstream response"));
        mvc.perform(get("/api/subscriptions"))
                .andExpect(status().isBadGateway())
                .andExpect(content().string(""));
    }
}
