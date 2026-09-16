package dev.pollito.flymcp.tools;

import java.util.Locale;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import dev.pollito.flymcp.brain.BrainBackend;
import dev.pollito.flymcp.brain.BrainBackend.ConsultResult;
import dev.pollito.flymcp.brain.BrainBackend.Pulse;
import dev.pollito.flymcp.brain.BrainBackend.Reinforcement;
import dev.pollito.flymcp.brain.BrainBackend.Vitals;
import dev.pollito.flymcp.brain.FrameRenderer;

/**
 * MCP tools: the fly's public face. Every description carries the same honesty
 * line, because the underlying readout is engineered, not cognition.
 */
@Component
public class FlyTools {

	private static final String HONESTY = "Engineered readout on spike data: comedy oracle, not intelligence.";

	private final BrainBackend brain;

	private final FrameRenderer renderer;

	public FlyTools(BrainBackend brain, FrameRenderer renderer) {
		this.brain = brain;
		this.renderer = renderer;
	}

	@McpTool(name = "ask_fly",
			description = "Ask the fly brain a question. The text is rendered into a light 320x180 frame and read "
					+ "through the MaleCNS v1.0 connectome's engineered DNp20 left/right spike differential and gate. "
					+ "Returns the BUY/SELL/HOLD verdict plus raw spike stats. " + HONESTY)
	public String askFly(
			@McpToolParam(description = "Question to show the fly, plain text", required = true) String text) {
		if (text == null || text.isBlank()) {
			throw new IllegalArgumentException("text must not be blank");
		}
		ConsultResult result = this.brain.consult(this.renderer.render(text), Reinforcement.NONE);
		String gate = result.gateSpikes() > 0 ? "gate open" : "gate closed";
		String consult = result.oracleConsult() == null ? "" : " | consult #" + result.oracleConsult();
		return String.format(Locale.ROOT, """
				verdict: %s (%s)
				spikes: left=%.3fHz right=%.3fHz diff=%+.3fHz gate=%d approach=%.3fHz
				brain: %s%s
				readout: engineered DNp20 left/right differential on MaleCNS v1.0 spike data; comedy oracle, not intelligence.""",
				result.side(), gate, result.leftHz(), result.rightHz(), result.differenceHz(), result.gateSpikes(),
				result.approachHz(), result.brainKind(), consult);
	}

	@McpTool(name = "reward_fly",
			description = "Queue a reward pulse (15 PAM11 dopamine cells) for the fly. The pulse is delivered "
					+ "during the next ask_fly consult, not at call time. " + HONESTY)
	public String rewardFly(@McpToolParam(description = "Short reason for the reward; logged and echoed back",
			required = false) String reason) {
		return pulseText("reward", this.brain.reward(reason), reason);
	}

	@McpTool(name = "punish_fly",
			description = "Queue an aversive pulse (2 PPL101 cells) for the fly. The pulse is delivered "
					+ "during the next ask_fly consult, not at call time. " + HONESTY)
	public String punishFly(@McpToolParam(description = "Short reason for the punishment; logged and echoed back",
			required = false) String reason) {
		return pulseText("aversive", this.brain.punish(reason), reason);
	}

	@McpTool(name = "fly_vitals",
			description = "Brain kind, consult count and memory stats for the fly backend. " + HONESTY)
	public String flyVitals() {
		Vitals vitals = this.brain.vitals();
		return String.format(Locale.ROOT, """
				brain: %s
				loaded: %s
				consults: %d
				pending_pulse: %s
				memory: %s""", vitals.brainKind(), vitals.loaded(), vitals.consultCount(),
				vitals.pendingReinforcement(), vitals.memory());
	}

	private String pulseText(String kind, Pulse pulse, String reason) {
		String reasonLine = (reason == null || reason.isBlank()) ? "" : "\nreason: " + reason.strip();
		return "queued: " + pulse.queued() + reasonLine + "\nconsumed_by: next ask_fly consult"
				+ "\nreadout: engineered reinforcement pulses, not modeled pleasure or pain; comedy oracle, not intelligence.";
	}

}
