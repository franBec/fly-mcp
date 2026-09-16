package dev.pollito.flymcp.prompts;

import java.util.List;

import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

@Component
public class FlyPrompts {

	@McpPrompt(name = "second-opinion",
			description = "Put a question to the fly through the ask_fly tool and report its verdict honestly.")
	public GetPromptResult secondOpinion(
			@McpArg(name = "question", description = "The question to put to the fly", required = true) String question,
			@McpArg(name = "context", description = "Optional context to fold into the question",
					required = false) String context) {
		StringBuilder message = new StringBuilder("Call the ask_fly tool with this question: ").append(question.strip());
		if (context != null && !context.isBlank()) {
			message.append("\n\nContext to include: ").append(context.strip());
		}
		message.append(
				"\n\nReport the verdict and the raw spike stats. State that the readout is engineered on "
						+ "MaleCNS spike data and that the fly is a comedy oracle, not intelligence. Do not "
						+ "dress it up as a prediction.");
		return GetPromptResult.builder(List.of(new PromptMessage(Role.USER, TextContent.builder(message.toString()).build())))
			.description("Second opinion from a MaleCNS v1.0 connectome readout")
			.build();
	}

}
