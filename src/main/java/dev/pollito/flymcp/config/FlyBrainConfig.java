package dev.pollito.flymcp.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import dev.pollito.flymcp.brain.BrainBackend;
import dev.pollito.flymcp.brain.FrameRenderer;
import dev.pollito.flymcp.brain.HttpBrainBackend;
import dev.pollito.flymcp.brain.MockBrainBackend;
import tools.jackson.databind.ObjectMapper;

/**
 * Picks the brain backend: the HTTP oracle when {@code flybrain.base-url} is
 * set, otherwise a deterministic mock so the app runs and tests with zero
 * infrastructure.
 */
@Configuration
@EnableConfigurationProperties(FlyBrainProperties.class)
public class FlyBrainConfig {

	@Bean
	BrainBackend brainBackend(ObjectMapper objectMapper, FlyBrainProperties properties) {
		if (StringUtils.hasText(properties.baseUrl())) {
			return new HttpBrainBackend(objectMapper, properties.baseUrl());
		}
		return new MockBrainBackend();
	}

	@Bean
	FrameRenderer frameRenderer() {
		return new FrameRenderer();
	}

}
