package com.asg.rocketflyiii.firefly.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class FireflyAccountResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesPaginationMetadata() {
        String json =
                """
                {
                  "data": [],
                  "meta": {
                    "pagination": {
                      "total": 750,
                      "count": 50,
                      "per_page": 50,
                      "current_page": 1,
                      "total_pages": 15
                    }
                  }
                }
                """;

        FireflyAccountResponse response =
                objectMapper.readValue(json, FireflyAccountResponse.class);

        assertEquals(750, response.meta().pagination().total());
        assertEquals(50, response.meta().pagination().per_page());
        assertEquals(1, response.meta().pagination().current_page());
        assertEquals(15, response.meta().pagination().total_pages());
        assertEquals(0, response.data().size());
    }
}
