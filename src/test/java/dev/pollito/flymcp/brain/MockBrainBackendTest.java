package dev.pollito.flymcp.brain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import dev.pollito.flymcp.brain.BrainBackend.ConsultResult;
import dev.pollito.flymcp.brain.BrainBackend.Pulse;
import dev.pollito.flymcp.brain.BrainBackend.Reinforcement;

class MockBrainBackendTest {

	private static final byte[] FRAME = { 1, 2, 3, 4, 5 };

	@Test
	void sameFrameInSameOrderReadsTheSame() {
		MockBrainBackend a = new MockBrainBackend();
		MockBrainBackend b = new MockBrainBackend();

		assertThat(a.consult(FRAME, Reinforcement.NONE)).isEqualTo(b.consult(FRAME, Reinforcement.NONE));
		assertThat(a.consult(FRAME, Reinforcement.NONE)).isEqualTo(b.consult(FRAME, Reinforcement.NONE));
	}

	@Test
	void callCounterMovesTheRead() {
		MockBrainBackend backend = new MockBrainBackend();

		ConsultResult first = backend.consult(FRAME, Reinforcement.NONE);
		ConsultResult second = backend.consult(FRAME, Reinforcement.NONE);

		assertThat(second).isNotEqualTo(first);
		assertThat(second.oracleConsult()).isEqualTo(2);
	}

	@Test
	void rewardPulseIsConsumedByTheNextConsult() {
		MockBrainBackend backend = new MockBrainBackend();

		Pulse pulse = backend.reward("good fly");
		assertThat(pulse.queued()).isEqualTo("reward");
		assertThat(pulse.consumedAtNextConsult()).isTrue();
		assertThat(backend.vitals().pendingReinforcement()).isEqualTo("reward");

		backend.consult(FRAME, Reinforcement.NONE);

		assertThat(backend.vitals().pendingReinforcement()).isEqualTo("none");
	}

	@Test
	void pulseChangesTheNextReadDeterministically() {
		MockBrainBackend rewarded = new MockBrainBackend();
		MockBrainBackend control = new MockBrainBackend();

		rewarded.reward("same frame, different wiring");
		ConsultResult withPulse = rewarded.consult(FRAME, Reinforcement.NONE);
		ConsultResult withoutPulse = control.consult(FRAME, Reinforcement.NONE);

		assertThat(withPulse.differenceHz()).isNotEqualTo(withoutPulse.differenceHz());
	}

	@Test
	void sideVocabularyStaysInTheOracleDialect() {
		MockBrainBackend backend = new MockBrainBackend();

		for (int i = 0; i < 20; i++) {
			assertThat(backend.consult(FRAME, Reinforcement.NONE).side()).isIn("BUY", "SELL", "HOLD");
		}
	}

	@Test
	void vitalsAndReportDescribeTheMock() {
		MockBrainBackend backend = new MockBrainBackend();

		assertThat(backend.vitals().brainKind()).isEqualTo("mock");
		assertThat(backend.vitals().consultCount()).isZero();
		assertThat(backend.report()).containsEntry("release", "mock").containsEntry("validated", false);
	}

}
