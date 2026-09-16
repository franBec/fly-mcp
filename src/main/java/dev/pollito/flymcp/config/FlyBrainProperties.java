package dev.pollito.flymcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flybrain")
public record FlyBrainProperties(String baseUrl) {
}
