package org.togetherjava.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Config(
        @JsonProperty("botTokens") List<String> botTokens,
        @JsonProperty("ignoreChannels") List<String> ignoreChannels,
        @JsonProperty("logsChannel") String logsChannel) {}
