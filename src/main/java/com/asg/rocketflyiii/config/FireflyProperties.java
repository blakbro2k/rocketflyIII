package com.asg.rocketflyiii.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rocketfly.firefly")
public record FireflyProperties(String baseUrl, String apiToken) {}
