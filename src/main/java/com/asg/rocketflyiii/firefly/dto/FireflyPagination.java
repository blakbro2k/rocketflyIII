package com.asg.rocketflyiii.firefly.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FireflyPagination(
        int total, int count, int per_page, int current_page, int total_pages) {}
