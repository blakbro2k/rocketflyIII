package com.asg.rocketflyiii.access;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.asg.rocketflyiii.firefly.api.FireflyClient;
import com.asg.rocketflyiii.firefly.dto.*;
import jakarta.servlet.Filter;
import java.nio.file.*;
import java.time.YearMonth;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:access-test;DB_CLOSE_DELAY=-1")
class ProfileAccessTest {
    @TempDir static Path directory;

    @DynamicPropertySource
    static void data(DynamicPropertyRegistry props) {
        props.add("rocketfly.data-dir", directory::toString);
    }

    @Autowired WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    Filter security;

    @Autowired AccessStore store;
    @MockitoBean FireflyClient firefly;
    MockMvc mvc;
    ObjectMapper json = new ObjectMapper();
    static final String PASSWORD = "demo-test-password";

    @BeforeEach
    void prepare() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    }

    MockHttpServletRequestBuilder withToken(
            MockHttpServletRequestBuilder request, MockHttpSession session) throws Exception {
        var response =
                mvc.perform(get("/api/csrf").session(session))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();
        var csrf = json.readTree(response.getContentAsString());
        return request.session(session)
                .header(csrf.path("header").asString(), csrf.path("token").asString());
    }

    MockHttpSession login(String username, String password) throws Exception {
        var session = new MockHttpSession();
        mvc.perform(
                        withToken(
                                post("/login")
                                        .param("username", username)
                                        .param("password", password),
                                session))
                .andExpect(redirectedUrl("/"));
        return session;
    }

    void claim(String token, String password, int status) throws Exception {
        mvc.perform(
                        withToken(
                                post("/api/setup")
                                        .contentType("application/json")
                                        .content(
                                                json.writeValueAsString(
                                                        Map.of(
                                                                "token",
                                                                token,
                                                                "username",
                                                                "owner",
                                                                "password",
                                                                password))),
                                new MockHttpSession()))
                .andExpect(status().is(status));
    }

    @Test
    void bootstrapAssignGrantRevokeAndRecover() throws Exception {
        mvc.perform(get("/")).andExpect(redirectedUrl("/setup.html"));
        mvc.perform(get("/api/budgets").servletPath("/api/budgets"))
                .andExpect(status().isUnauthorized());
        Files.writeString(directory.resolve("setup-token"), "first-token");
        claim("wrong", PASSWORD, 403);
        claim("first-token", PASSWORD, 200);
        claim("first-token", PASSWORD, 403);
        var admin = login("owner", PASSWORD);
        mvc.perform(get("/api/admin/profiles").session(admin)).andExpect(status().isOk());
        var familyInput =
                new AccessStore.RoleInput("Family", Set.of(new AccessStore.Grant("budget", "12")));
        mvc.perform(
                        withToken(
                                post("/api/admin/roles")
                                        .contentType("application/json")
                                        .content(json.writeValueAsString(familyInput)),
                                admin))
                .andExpect(status().isOk());
        var role =
                store.roles().stream()
                        .filter(r -> r.name().equals("Family"))
                        .findFirst()
                        .orElseThrow();
        mvc.perform(
                        withToken(
                                post("/api/admin/profiles")
                                        .contentType("application/json")
                                        .content(
                                                json.writeValueAsString(
                                                        Map.of(
                                                                "username",
                                                                "family",
                                                                "password",
                                                                PASSWORD,
                                                                "roleId",
                                                                role.id()))),
                                admin))
                .andExpect(status().isOk());
        var family = login("family", PASSWORD);
        mvc.perform(get("/api/me").session(family))
                .andExpect(jsonPath("$.roleName").value("Family"));
        mvc.perform(get("/api/admin/resources").session(family)).andExpect(status().isForbidden());
        mvc.perform(get("/api/subscriptions").session(family)).andExpect(status().isForbidden());
        mvc.perform(get("/api/accounts/12/activity?month=2026-11").session(family))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/budgets/13/activity?month=2026-11").session(family))
                .andExpect(status().isForbidden());
        var meta = new FireflyMeta(new FireflyPagination(2, 2, 50, 1, 1));
        when(firefly.getBudgetsPage(1))
                .thenReturn(
                        new FireflyPage(
                                List.of(
                                        json.readTree("{\"id\":\"12\"}"),
                                        json.readTree("{\"id\":\"13\"}")),
                                meta));
        mvc.perform(get("/api/budgets").session(family))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("12"));
        var empty = new FireflyPage(List.of(), meta);
        when(firefly.getAccountsPage("asset", 1)).thenReturn(empty);
        when(firefly.getAccountsPage("liability", 1)).thenReturn(empty);
        mvc.perform(get("/api/admin/resources").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kind").value("budget"));
        when(firefly.getBudgetLimits(12, YearMonth.of(2026, 11), 1)).thenReturn(empty);
        when(firefly.getBudgetTransactions(12, YearMonth.of(2026, 11), 1)).thenReturn(empty);
        mvc.perform(get("/api/budgets/12/activity?month=2026-11").session(family))
                .andExpect(status().isOk());
        mvc.perform(
                        withToken(
                                put("/api/admin/roles/" + role.id())
                                        .contentType("application/json")
                                        .content(
                                                json.writeValueAsString(
                                                        new AccessStore.RoleInput(
                                                                "Family", Set.of()))),
                                admin))
                .andExpect(status().isOk());
        mvc.perform(get("/api/budgets/12/activity?month=2026-11").session(family))
                .andExpect(status().isForbidden());
        mvc.perform(withToken(delete("/api/admin/roles/" + role.id()), admin))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/admin/roles/" + role.id()).session(admin))
                .andExpect(status().isForbidden());
        String path = "/api/admin/profiles/family";
        String inactive = json.writeValueAsString(Map.of("roleId", role.id(), "active", false));
        mvc.perform(withToken(put(path).contentType("application/json").content(inactive), family))
                .andExpect(status().isForbidden());
        mvc.perform(withToken(put(path).contentType("application/json").content(inactive), admin))
                .andExpect(status().isOk());
        mvc.perform(get("/api/me").session(family)).andExpect(status().isUnauthorized());
        mvc.perform(withToken(post("/login").param("username", "family").param("password", PASSWORD),
                        new MockHttpSession()))
                .andExpect(redirectedUrl("/login?error"));
        mvc.perform(withToken(put("/api/admin/profiles/owner").contentType("application/json")
                        .content(inactive), admin))
                .andExpect(status().isBadRequest());
        String active = json.writeValueAsString(Map.of("roleId", role.id(), "active", true));
        mvc.perform(withToken(put(path).contentType("application/json").content(active), admin))
                .andExpect(status().isOk());
        mvc.perform(get("/api/me").session(login("family", PASSWORD)))
                .andExpect(jsonPath("$.active").value(true));
        Files.writeString(directory.resolve("setup-token"), "recovery-token");
        claim("recovery-token", "replacement-password", 200);
        mvc.perform(get("/api/admin/profiles").session(admin)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/profiles").session(login("owner", "replacement-password")))
                .andExpect(jsonPath("$.length()").value(2));
    }
}
