package dev.pollito.flymcp.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpSurfaceTest {

	@LocalServerPort
	private int port;

	@Test
	void registersTheFullFlySurfaceOverStreamableHttp() {
		var transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + this.port)
			.endpoint("/mcp")
			.build();
		McpSyncClient client = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(30)).build();
		try {
			client.initialize();

			assertThat(client.listTools().tools()).extracting(McpSchema.Tool::name)
				.containsExactlyInAnyOrder("ask_fly", "reward_fly", "punish_fly", "fly_vitals");
			assertThat(client.listResources().resources()).extracting(McpSchema.Resource::uri)
				.containsExactly("fly://vitals");
			assertThat(client.listPrompts().prompts()).extracting(McpSchema.Prompt::name)
				.containsExactly("second-opinion");

			McpSchema.CallToolResult consult = client.callTool(McpSchema.CallToolRequest.builder("ask_fly")
				.arguments(Map.of("text", "Is this thing on?"))
				.build());
			assertThat(text(consult)).contains("verdict:").contains("gate=").contains("comedy oracle");

			McpSchema.CallToolResult vitals = client.callTool(McpSchema.CallToolRequest.builder("fly_vitals").build());
			assertThat(text(vitals)).contains("brain: mock");

			McpSchema.ReadResourceResult resource = client
				.readResource(McpSchema.ReadResourceRequest.builder("fly://vitals").build());
			assertThat(resource.contents()).hasSize(1);

			McpSchema.GetPromptResult prompt = client.getPrompt(McpSchema.GetPromptRequest.builder("second-opinion")
				.arguments(Map.of("question", "ship it?"))
				.build());
			assertThat(prompt.messages()).hasSize(1);
		}
		finally {
			client.close();
		}
	}

	private String text(McpSchema.CallToolResult result) {
		assertThat(result.content()).hasSize(1);
		assertThat(result.content().get(0)).isInstanceOf(McpSchema.TextContent.class);
		return ((McpSchema.TextContent) result.content().get(0)).text();
	}

}
