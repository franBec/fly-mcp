package dev.pollito.flymcp.resources;

import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import dev.pollito.flymcp.brain.BrainBackend;
import tools.jackson.databind.ObjectMapper;

@Component
public class FlyResources {

	private final BrainBackend brain;

	private final ObjectMapper mapper;

	public FlyResources(BrainBackend brain, ObjectMapper mapper) {
		this.brain = brain;
		this.mapper = mapper;
	}

	@McpResource(uri = "fly://vitals", name = "fly-vitals", title = "Fly vitals",
			description = "Brain kind, consult count and memory stats as JSON. Engineered readout on spike data: "
					+ "comedy oracle, not intelligence.",
			mimeType = "application/json")
	public String vitals() {
		return this.mapper.writeValueAsString(this.brain.vitals());
	}

}
