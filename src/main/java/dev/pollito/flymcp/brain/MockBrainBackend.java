package dev.pollito.flymcp.brain;

import java.util.Arrays;
import java.util.Map;

/**
 * Deterministic stand-in for the real oracle: scores come from the frame bytes
 * plus a consult counter, never from clocks or randomness. Same frames in the
 * same order produce the same reads, which makes the repo testable with zero
 * infrastructure.
 *
 * <p>It is deliberately not a model of the fly. It exists to smoke-test
 * plumbing and to keep the test contract stable.
 */
public class MockBrainBackend implements BrainBackend {

	private static final String KIND = "mock";

	private long consults;

	private Reinforcement pending = Reinforcement.NONE;

	@Override
	public synchronized ConsultResult consult(byte[] png, Reinforcement reinforcement) {
		Reinforcement effective = (reinforcement != null && reinforcement != Reinforcement.NONE) ? reinforcement
				: this.pending;
		this.pending = Reinforcement.NONE;

		int pulseSalt = effective == Reinforcement.REWARD ? 7 : effective == Reinforcement.AVERSIVE ? -11 : 0;
		int mixed = Arrays.hashCode(png == null ? new byte[0] : png) ^ (int) (this.consults * 0x9E3779B9L) ^ pulseSalt;
		double left = Math.max(0.0, 1.5 + (((mixed) & 0xFF) - 128) / 40.0);
		double right = Math.max(0.0, 1.5 + (((mixed >>> 8) & 0xFF) - 128) / 40.0);
		double difference = right - left;
		int gate = Math.abs(difference) > 0.4 ? 1 : 0;
		String side = (gate == 0 || Math.abs(difference) < 2.0) ? "HOLD" : (difference > 0 ? "BUY" : "SELL");

		this.consults++;
		return new ConsultResult(side, round(left), round(right), round(difference), gate, round(left + right), KIND,
				this.consults);
	}

	@Override
	public synchronized Pulse reward(String reason) {
		this.pending = Reinforcement.REWARD;
		return new Pulse(Reinforcement.REWARD.wire(), true);
	}

	@Override
	public synchronized Pulse punish(String reason) {
		this.pending = Reinforcement.AVERSIVE;
		return new Pulse(Reinforcement.AVERSIVE.wire(), true);
	}

	@Override
	public synchronized Vitals vitals() {
		return new Vitals(KIND, true, this.consults, this.pending.wire(),
				Map.of("model", KIND, "plastic_edges", 7835, "changed_edges", 0));
	}

	@Override
	public Map<String, Object> report() {
		return Map.of("release", KIND, "neurons", 166700, "note",
				"MockBrainBackend: deterministic plumbing test double, not a fly.", "validated", false);
	}

	private static double round(double value) {
		return Math.round(value * 1000.0) / 1000.0;
	}

}
