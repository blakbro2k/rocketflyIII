package com.asg.rocketflyiii.firefly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.asg.rocketflyiii.firefly.dto.FireflyAccountResponse;
import com.asg.rocketflyiii.firefly.dto.FireflyMeta;
import com.asg.rocketflyiii.firefly.dto.FireflyPagination;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class FireflyAccountServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void retrievesAllAccountPages() {
        FireflyClient client = mock(FireflyClient.class);

        when(client.getAccountsPage(1)).thenReturn(response(1, 3, "account-1"));
        when(client.getAccountsPage(2)).thenReturn(response(2, 3, "account-2"));
        when(client.getAccountsPage(3)).thenReturn(response(3, 3, "account-3"));

        FireflyAccountService service = new FireflyAccountService(client);

        List<JsonNode> accounts = service.getAllAccounts();

        assertEquals(3, accounts.size());
        assertEquals("account-1", accounts.get(0).get("id").asText());
        assertEquals("account-2", accounts.get(1).get("id").asText());
        assertEquals("account-3", accounts.get(2).get("id").asText());

        verify(client).getAccountsPage(1);
        verify(client).getAccountsPage(2);
        verify(client).getAccountsPage(3);
        verifyNoMoreInteractions(client);
    }

    private FireflyAccountResponse response(int currentPage, int totalPages, String accountId) {

        JsonNode account = mapper.readTree("{\"id\":\"" + accountId + "\"}");

        FireflyPagination pagination =
                new FireflyPagination(totalPages, 1, 1, currentPage, totalPages);

        return new FireflyAccountResponse(List.of(account), new FireflyMeta(pagination));
    }

    @Test
    void retrievesSinglePageWithoutAdditionalRequests() {
        FireflyClient client = mock(FireflyClient.class);

        when(client.getAccountsPage(1)).thenReturn(response(1, 1, "account-1"));

        FireflyAccountService service = new FireflyAccountService(client);

        List<JsonNode> accounts = service.getAllAccounts();

        assertEquals(1, accounts.size());
        assertEquals("account-1", accounts.get(0).get("id").asText());

        verify(client).getAccountsPage(1);
        verifyNoMoreInteractions(client);
    }
}
