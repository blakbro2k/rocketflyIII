package com.asg.rocketflyiii.access;

import org.springframework.http.MediaType;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
public class LoginController {
    @GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE)
    public String login(CsrfToken csrf, @RequestParam(required = false) String error) {
        return """
                <!doctype html>
                <html lang="en"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Sign in · RocketFlyIII</title><link rel="stylesheet" href="accounts.css">
                </head><body><header><span class="brand"><img src="rocket-mark.svg" alt="">RocketFlyIII</span></header>
                <main class="panel access-page"><h1>Sign in</h1>%s
                <form method="post" action="login">
                <input type="hidden" name="%s" value="%s">
                <label>Username <input name="username" autocomplete="username" required></label>
                <label>Password <input type="password" name="password" autocomplete="current-password" required></label>
                <button>Sign in</button></form></main></body></html>
                """.formatted(
                error == null ? "" : "<p role=\"alert\">Unable to sign in with these credentials.</p>",
                HtmlUtils.htmlEscape(csrf.getParameterName()),
                HtmlUtils.htmlEscape(csrf.getToken()));
    }
}
