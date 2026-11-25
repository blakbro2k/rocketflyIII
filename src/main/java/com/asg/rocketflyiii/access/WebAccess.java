package com.asg.rocketflyiii.access;

import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.*;
import org.springframework.web.servlet.config.annotation.*;
import tools.jackson.databind.JsonNode;

@Configuration
public class WebAccess implements WebMvcConfigurer, HandlerInterceptor {
    private final AccessStore store;

    public WebAccess(AccessStore store) {
        this.store = store;
    }

    @Bean
    static PasswordEncoder passwords() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(
                        a ->
                                a.requestMatchers(
                                                "/setup.html",
                                                "/setup.js",
                                                "/accounts.css",
                                                "/access-ui.js",
                                                "/api/csrf",
                                                "/api/setup",
                                                "/error")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        errors ->
                                errors.authenticationEntryPoint(
                                        (request, response, exception) -> {
                                            if (request.getServletPath().startsWith("/api/"))
                                                response.sendError(401);
                                            else
                                                response.sendRedirect(
                                                        request.getContextPath()
                                                                + (store.needsSetup()
                                                                        ? "/setup.html"
                                                                        : "/login"));
                                        }))
                .formLogin(f -> f.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
                .logout(l -> l.logoutSuccessUrl("/login"))
                .build();
    }

    public record Scope(boolean admin, Set<AccessStore.Grant> grants) {
        public boolean allows(String kind, String id) {
            return admin || grants.contains(new AccessStore.Grant(kind, id));
        }

        public String visibleName(JsonNode split, String field, String kind) {
            if (!allows(kind, split.path(field + "_id").asText(""))) return null;
            return split.path(field + "_name").asText(null);
        }

        public List<JsonNode> visible(String kind, List<JsonNode> items) {
            return admin
                    ? items
                    : items.stream()
                            .filter(item -> allows(kind, item.path("id").asString("")))
                            .toList();
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this)
                .addPathPatterns("/api/**", "/roles.html")
                .excludePathPatterns("/api/setup", "/api/csrf");
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler)
            throws Exception {
        var auth = (Authentication) req.getUserPrincipal();
        if (auth == null) {
            res.sendError(401);
            return false;
        }
        var profile = store.profile(auth.getName());
        if (!profile.active() || auth.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("version:" + profile.version()))) {
            req.getSession().invalidate();
            res.sendError(401);
            return false;
        }
        var scope =
                new Scope(
                        profile.admin(),
                        profile.admin() ? Set.of() : Set.copyOf(store.grants(profile.roleId())));
        req.setAttribute("scope", scope);
        req.setAttribute("profile", profile);
        String pattern =
                String.valueOf(req.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
        if (profile.admin() || Set.of("/api/me", "/api/accounts", "/api/budgets").contains(pattern))
            return true;
        var vars = (Map<?, ?>) req.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (vars != null
                && (pattern.equals("/api/accounts/{id}/activity")
                        || pattern.equals("/api/budgets/{id}/activity"))) {
            String kind = pattern.startsWith("/api/accounts/") ? "account" : "budget";
            if (scope.allows(kind, String.valueOf(vars.get("id")))) return true;
        }
        res.sendError(HttpStatus.FORBIDDEN.value());
        return false;
    }
}
