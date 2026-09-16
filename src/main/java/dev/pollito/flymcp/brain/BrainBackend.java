package dev.pollito.flymcp.brain;

import java.util.Map;

/**
 * Contract between the MCP surface and whatever actually answers a consult.
 *
 * <p>The only shipped implementation of the real brain is the vendored Python
 * oracle. Everything here is an engineered readout on MaleCNS v1.0 spike data:
 * a comedy oracle, not intelligence.
 */
public interface BrainBackend {

	enum Reinforcement {
		NONE("none"), REWARD("reward"), AVERSIVE("aversive");

		private final String wire;

		Reinforcement(String wire) {
			this.wire = wire;
		}

		public String wire() {
			return this.wire;
		}
	}

	record ConsultResult(String side, double leftHz, double rightHz, double differenceHz, int gateSpikes,
			double approachHz, String brainKind, Long oracleConsult) {
	}

	record Pulse(String queued, boolean consumedAtNextConsult) {
	}

	record Vitals(String brainKind, boolean loaded, long consultCount, String pendingReinforcement,
			Map<String, Object> memory) {
	}

	/**
	 * Run one frame through the brain. Reinforcement pulses queued with
	 * {@link #reward(String)} or {@link #punish(String)} are consumed here.
	 */
	ConsultResult consult(byte[] png, Reinforcement reinforcement);

	/** Queue a reward pulse for the next consult. */
	Pulse reward(String reason);

	/** Queue an aversive pulse for the next consult. */
	Pulse punish(String reason);

	/** Brain state, consult count and memory stats. */
	Vitals vitals();

	/** Raw circuit report as served by the backend. */
	Map<String, Object> report();

}
