package dev.pollito.flymcp.brain;

import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Talks to the vendored fly oracle over HTTP. Consult latency on the real
 * MaleCNS connectome averages a few seconds, so timeouts are generous.
 */
public class HttpBrainBackend implements BrainBackend {

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

	private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(90);

	private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
	};

	private final WebClient webClient;

	private final ObjectMapper mapper;

	private final String baseUrl;

	public HttpBrainBackend(ObjectMapper mapper, String baseUrl) {
		HttpClient httpClient = HttpClient.create()
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) CONNECT_TIMEOUT.toMillis())
			.responseTimeout(RESPONSE_TIMEOUT);
		this.webClient = WebClient.builder()
			.baseUrl(baseUrl)
			.clientConnector(new ReactorClientHttpConnector(httpClient))
			.build();
		this.mapper = mapper;
		this.baseUrl = baseUrl;
	}

	@Override
	public ConsultResult consult(byte[] png, Reinforcement reinforcement) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("png_b64", Base64.getEncoder().encodeToString(png));
		if (reinforcement != null && reinforcement != Reinforcement.NONE) {
			body.put("reinforcement", reinforcement.wire());
		}
		JsonNode res = post("/consult", body, "consult");
		return new ConsultResult(res.path("side").asString("HOLD"), res.path("left_hz").asDouble(),
				res.path("right_hz").asDouble(), res.path("difference_hz").asDouble(),
				res.path("gate_spikes").asInt(), res.path("approach_hz").asDouble(),
				res.path("brain_kind").asString("unknown"),
				res.hasNonNull("oracle_consult") ? res.get("oracle_consult").asLong() : null);
	}

	@Override
	public Pulse reward(String reason) {
		return pulse("/reward", Reinforcement.REWARD);
	}

	@Override
	public Pulse punish(String reason) {
		return pulse("/punish", Reinforcement.AVERSIVE);
	}

	@Override
	public Vitals vitals() {
		JsonNode res = get("/vitals", "vitals");
		Map<String, Object> memory = res.hasNonNull("memory")
				? this.mapper.convertValue(res.get("memory"), MAP_TYPE) : Map.of();
		return new Vitals(res.path("brain_kind").asString("unknown"), res.path("loaded").asBoolean(true),
				res.path("consult_count").asLong(0), res.path("pending_reinforcement").asString("none"), memory);
	}

	@Override
	public Map<String, Object> report() {
		return this.mapper.convertValue(get("/report", "report"), MAP_TYPE);
	}

	private Pulse pulse(String uri, Reinforcement reinforcement) {
		JsonNode res = post(uri, null, uri.substring(1));
		return new Pulse(res.path("queued").asString(reinforcement.wire()),
				res.path("consumed_at_next_consult").asBoolean(true));
	}

	private JsonNode get(String uri, String operation) {
		try {
			JsonNode res = this.webClient.get().uri(uri).retrieve().bodyToMono(JsonNode.class).block();
			return requireBody(res, operation);
		}
		catch (IllegalStateException ex) {
			throw ex;
		}
		catch (WebClientResponseException ex) {
			throw new IllegalStateException(errorMessage(operation, ex), ex);
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException(unreachableMessage(operation, ex), ex);
		}
	}

	private JsonNode post(String uri, Object body, String operation) {
		try {
			WebClient.RequestBodySpec spec = this.webClient.post().uri(uri)
				.contentType(MediaType.APPLICATION_JSON);
			WebClient.RequestHeadersSpec<?> request = (body != null ? spec.bodyValue(body) : spec);
			JsonNode res = request.retrieve().bodyToMono(JsonNode.class).block();
			return requireBody(res, operation);
		}
		catch (IllegalStateException ex) {
			throw ex;
		}
		catch (WebClientResponseException ex) {
			throw new IllegalStateException(errorMessage(operation, ex), ex);
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException(unreachableMessage(operation, ex), ex);
		}
	}

	private JsonNode requireBody(JsonNode res, String operation) {
		if (res == null || res.isMissingNode()) {
			throw new IllegalStateException("fly oracle " + operation + " at " + this.baseUrl + " returned no body");
		}
		return res;
	}

	private String errorMessage(String operation, WebClientResponseException ex) {
		return "fly oracle " + operation + " failed: HTTP " + ex.getStatusCode().value() + " "
				+ ex.getResponseBodyAsString();
	}

	private String unreachableMessage(String operation, RuntimeException ex) {
		return "fly oracle " + operation + " at " + this.baseUrl + " failed: " + ex.getMessage();
	}

}
