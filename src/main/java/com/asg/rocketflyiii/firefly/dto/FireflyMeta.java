package com.asg.rocketflyiii.firefly.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FireflyMeta(FireflyPagination pagination) {}
