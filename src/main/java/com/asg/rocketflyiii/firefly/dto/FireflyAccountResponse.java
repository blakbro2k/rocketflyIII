package com.asg.rocketflyiii.firefly.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import tools.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FireflyAccountResponse(List<JsonNode> data, FireflyMeta meta) {}
