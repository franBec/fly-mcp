package dev.pollito.flymcp.brain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.pollito.flymcp.brain.BrainBackend.ConsultResult;
import dev.pollito.flymcp.brain.BrainBackend.Pulse;
import dev.pollito.flymcp.brain.BrainBackend.Reinforcement;
import dev.pollito.flymcp.brain.BrainBackend.Vitals;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class HttpBrainBackendTest {

	private MockWebServer server;

	private ObjectMapper mapper;

	private HttpBrainBackend backend;

	@BeforeEach
	void setUp() throws Exception {
		this.server = new MockWebServer();
		this.server.start();
		this.mapper = new ObjectMapper();
		this.backend = new HttpBrainBackend(this.mapper, this.server.url("/").toString());
	}

	@AfterEach
	void tearDown() throws Exception {
		this.server.shutdown();
	}

	@Test
	void consultSendsTheFrameAndParsesTheRead() throws Exception {
		enqueue("""
				{"side":"BUY","left_hz":1.2,"right_hz":4.5,"difference_hz":3.3,"gate_spikes":1,
				 "approach_hz":5.7,"brain_kind":"real-malecns-v1.0","oracle_consult":7}""");

		ConsultResult result = this.backend.consult(new byte[] { 9, 8, 7 }, Reinforcement.NONE);

		RecordedRequest request = this.server.takeRequest();
		assertThat(request.getPath()).isEqualTo("/consult");
		JsonNode body = this.mapper.readTree(request.getBody().readUtf8());
		assertThat(body.path("png_b64").asString())
			.isEqualTo(Base64.getEncoder().encodeToString(new byte[] { 9, 8, 7 }));
		assertThat(body.has("reinforcement")).isFalse();
		assertThat(result.side()).isEqualTo("BUY");
		assertThat(result.leftHz()).isEqualTo(1.2);
		assertThat(result.rightHz()).isEqualTo(4.5);
		assertThat(result.differenceHz()).isEqualTo(3.3);
		assertThat(result.gateSpikes()).isEqualTo(1);
		assertThat(result.approachHz()).isEqualTo(5.7);
		assertThat(result.brainKind()).isEqualTo("real-malecns-v1.0");
		assertThat(result.oracleConsult()).isEqualTo(7);
	}

	@Test
	void consultPassesExplicitReinforcement() throws Exception {
		enqueue("""
				{"side":"HOLD","left_hz":1.0,"right_hz":1.1,"difference_hz":0.1,"gate_spikes":0,"approach_hz":2.1}""");

		this.backend.consult(new byte[] { 1 }, Reinforcement.REWARD);

		JsonNode body = this.mapper.readTree(this.server.takeRequest().getBody().readUtf8());
		assertThat(body.path("reinforcement").asString()).isEqualTo("reward");
	}

	@Test
	void rewardAndPunishQueuePulses() throws Exception {
		enqueue("""
				{"queued":"reward","consumed_at_next_consult":true}""");
		enqueue("""
				{"queued":"aversive","consumed_at_next_consult":true}""");

		Pulse reward = this.backend.reward("good fly");
		Pulse punish = this.backend.punish("bad fly");

		assertThat(this.server.takeRequest().getPath()).isEqualTo("/reward");
		assertThat(this.server.takeRequest().getPath()).isEqualTo("/punish");
		assertThat(reward.queued()).isEqualTo("reward");
		assertThat(punish.queued()).isEqualTo("aversive");
		assertThat(reward.consumedAtNextConsult()).isTrue();
	}

	@Test
	void vitalsParseMemoryAndPendingPulse() throws Exception {
		enqueue("""
				{"brain_kind":"real-malecns-v1.0","loaded":true,"consult_count":3,
				 "pending_reinforcement":"reward","memory":{"model":"real","changed_edges":4}}""");

		Vitals vitals = this.backend.vitals();

		assertThat(this.server.takeRequest().getPath()).isEqualTo("/vitals");
		assertThat(vitals.brainKind()).isEqualTo("real-malecns-v1.0");
		assertThat(vitals.loaded()).isTrue();
		assertThat(vitals.consultCount()).isEqualTo(3);
		assertThat(vitals.pendingReinforcement()).isEqualTo("reward");
		assertThat(vitals.memory()).containsEntry("model", "real").containsEntry("changed_edges", 4);
	}

	@Test
	void reportReturnsTheRawCircuitMap() throws Exception {
		enqueue("""
				{"release":"mock","neurons":166700,"validated":false}""");

		assertThat(this.backend.report()).containsEntry("release", "mock").containsEntry("neurons", 166700);
	}

	@Test
	void serverErrorsBecomeActionableExceptions() {
		this.server.enqueue(new MockResponse().setResponseCode(500).setBody("kernel exploded"));

		assertThatThrownBy(() -> this.backend.consult(new byte[] { 1 }, Reinforcement.NONE))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("HTTP 500")
			.hasMessageContaining("kernel exploded");
	}

	private void enqueue(String body) {
		this.server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody(body));
	}

}
