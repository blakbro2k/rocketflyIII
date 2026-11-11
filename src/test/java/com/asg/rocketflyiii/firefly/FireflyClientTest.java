package com.asg.rocketflyiii.firefly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.asg.rocketflyiii.config.FireflyProperties;
import com.asg.rocketflyiii.firefly.dto.FireflyAccountResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class FireflyClientTest {

    @Test
    void getAboutSendsAuthenticatedRequest() {
        String baseUrl = "https://firefly.example.test";
        String token = "test-token";

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        FireflyProperties properties = new FireflyProperties(baseUrl, token);

        FireflyClient client = new FireflyClient(builder, properties);

        String responseBody = "{\"data\":{\"version\":\"6.7.9\"}}";

        server.expect(requestTo(baseUrl + FireflyEndpoints.ABOUT))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        String response = client.getAbout();

        assertEquals(responseBody, response);
        server.verify();
    }

    @Test
    void getAccountsSendsAuthenticatedRequest() {
        String baseUrl = "https://firefly.example.test";
        String token = "test-token";

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        FireflyClient client = new FireflyClient(builder, new FireflyProperties(baseUrl, token));

        String responseBody = "{\"data\":[]}";

        server.expect(requestTo(baseUrl + FireflyEndpoints.ACCOUNTS))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        assertEquals(responseBody, client.getAccounts());
        server.verify();
    }

    @Test
    void getAccountsRequestsSpecificPage() {
        String baseUrl = "https://firefly.example.test";
        String token = "test-token";

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        FireflyClient client = new FireflyClient(builder, new FireflyProperties(baseUrl, token));

        String responseBody = "{\"data\":[]}";

        server.expect(requestTo(baseUrl + FireflyEndpoints.ACCOUNTS + "?page=2"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        assertEquals(responseBody, client.getAccounts(2));
        server.verify();
    }

    @Test
    void getAccountsRejectsInvalidPageNumbers() {
        RestClient.Builder builder = RestClient.builder();

        FireflyClient client =
                new FireflyClient(
                        builder,
                        new FireflyProperties("https://firefly.example.test", "test-token"));

        assertThrows(IllegalArgumentException.class, () -> client.getAccounts(0));
        assertThrows(IllegalArgumentException.class, () -> client.getAccounts(-1));
    }

    @Test
    void getAccountsPageDeserializesResponse() {
        String baseUrl = "https://firefly.example.test";
        String token = "test-token";

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        FireflyClient client = new FireflyClient(builder, new FireflyProperties(baseUrl, token));

        String responseBody =
                """
            {
              "data": [],
              "meta": {
                "pagination": {
                  "total": 750,
                  "count": 50,
                  "per_page": 50,
                  "current_page": 2,
                  "total_pages": 15
                }
              }
            }
            """;

        server.expect(requestTo(baseUrl + FireflyEndpoints.ACCOUNTS + "?page=2"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        FireflyAccountResponse response = client.getAccountsPage(2);

        assertEquals(2, response.meta().pagination().current_page());
        assertEquals(15, response.meta().pagination().total_pages());
        assertEquals(750, response.meta().pagination().total());
        assertEquals(0, response.data().size());

        server.verify();
    }
}
