package com.asg.rocketflyiii.firefly;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.asg.rocketflyiii.config.FireflyProperties;
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
                    .build();

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
                {"data":%s,"meta":{"pagination":{"current_page":%d,"total_pages":%d}}}
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
