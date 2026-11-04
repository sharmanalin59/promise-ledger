package com.promiseledger.extraction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ClaudeClient {

	private static final Logger logger = LoggerFactory.getLogger(ClaudeClient.class);

	private static final String API_VERSION = "2023-06-01";

	private static final Set<Integer> RETRYABLE_STATUSES = Set.of(408, 429, 500, 502, 503, 504, 529);

	private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000L);

	private final ClaudeProperties properties;

	private final RestClient restClient;

	private final Clock clock;

	private final ClaudeSleeper sleeper;

	private final Random random;

	private final ClaudeCircuitBreaker circuit;

	private final ClaudeConcurrencyLimiter concurrency;

	@Autowired
	public ClaudeClient(ClaudeProperties properties) {
		this(properties, RestClient.builder().requestFactory(requestFactory(properties)));
	}

	ClaudeClient(ClaudeProperties properties, RestClient.Builder restClientBuilder) {
		this(properties, restClientBuilder, Clock.systemUTC(), ClaudeClient::sleepQuietly, ThreadLocalRandom.current());
	}

	ClaudeClient(ClaudeProperties properties, RestClient.Builder restClientBuilder, Clock clock, ClaudeSleeper sleeper,
			Random random) {
		this.properties = properties;
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl())
			.defaultHeader("anthropic-version", API_VERSION)
			.build();
		this.clock = clock;
		this.sleeper = sleeper;
		this.random = random;
		this.circuit = new ClaudeCircuitBreaker(properties.circuitFailureThreshold(), properties.circuitOpenDuration(),
				clock);
		this.concurrency = new ClaudeConcurrencyLimiter(properties.maxConcurrentCalls(), properties.concurrencyWait());
	}

	public boolean isConfigured() {
		return this.properties.hasApiKey();
	}

	/**
	 * Sends one user message and returns the concatenated text blocks of Claude's reply.
	 */
	public String complete(String systemPrompt, String userMessage) {
		return completeDetailed(systemPrompt, userMessage).text();
	}

	/**
	 * Sends one user message, retrying transient failures, and returns the reply with token usage when Claude
	 * provides it.
	 */
	public ClaudeCompletion completeDetailed(String systemPrompt, String userMessage) {
		if (!isConfigured()) {
			throw new ClaudeException("Claude API key is not configured.", null, ClaudeFailureCategory.NOT_CONFIGURED,
					0, null, null, null, null);
		}
		int estimatedTokens = estimateTokens(systemPrompt, userMessage);
		if (estimatedTokens > this.properties.maxInputTokens()) {
			throw new ClaudeInputTooLargeException(estimatedTokens, this.properties.maxInputTokens());
		}
		this.circuit.beforeCall();
		boolean acquired = false;
		long started = System.nanoTime();
		try {
			this.concurrency.acquire();
			acquired = true;
			return callWithRetries(systemPrompt, userMessage, started);
		}
		catch (ClaudeBusyException ex) {
			this.circuit.abandonTrial();
			throw ex;
		}
		finally {
			if (acquired) {
				this.concurrency.release();
			}
		}
	}

	static int estimateTokens(String systemPrompt, String userMessage) {
		int characters = length(systemPrompt) + length(userMessage);
		return (characters + 3) / 4;
	}

	private ClaudeCompletion callWithRetries(String systemPrompt, String userMessage, long started) {
		ClaudeException last = null;
		for (int attempt = 1; attempt <= this.properties.maxAttempts(); attempt++) {
			try {
				MessagesResponse response = send(systemPrompt, userMessage);
				if ("max_tokens".equals(response.stopReason())) {
					throw truncated(response);
				}
				ClaudeCompletion completion = toCompletion(response, attempt, started);
				this.circuit.recordSuccess();
				return completion;
			}
			catch (ClaudeException ex) {
				last = ex.withAttempts(attempt);
				boolean retry = last.category().retryable() && attempt < this.properties.maxAttempts();
				if (!retry) {
					if (last.category().countsTowardCircuit()) {
						this.circuit.recordFailure();
					}
					else {
						this.circuit.recordProviderResponded();
					}
					throw last;
				}
				Duration delay = ClaudeRetryDelay.next(attempt, this.properties.retryInitialBackoff(),
						this.properties.retryMaxBackoff(), this.random, last.retryAfter());
				logger.warn(
						"Retrying Claude call: model={}, attempt={}/{}, category={}, waitMs={}. The API key is not logged.",
						this.properties.model(), attempt, this.properties.maxAttempts(), last.category(),
						delay.toMillis());
				sleep(delay);
			}
		}
		throw last;
	}

	private MessagesResponse send(String systemPrompt, String userMessage) {
		MessagesRequest request = new MessagesRequest(this.properties.model(), this.properties.maxTokens(),
				systemPrompt, List.of(new Message("user", userMessage)));
		MessagesResponse response;
		try {
			response = this.restClient.post()
				.uri("/v1/messages")
				.header("x-api-key", this.properties.apiKey())
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(MessagesResponse.class);
		}
		catch (RestClientResponseException ex) {
			int status = ex.getStatusCode().value();
			throw new ClaudeException("Claude API returned HTTP " + status + ".", ex, categoryForStatus(status), 1,
					retryAfter(ex), null, null, null);
		}
		catch (ResourceAccessException ex) {
			if (isTimeout(ex)) {
				throw new ClaudeTimeoutException(
						"Claude API did not respond within " + this.properties.timeout().toSeconds() + " seconds.", ex);
			}
			throw new ClaudeException("Could not reach the Claude API.", ex, ClaudeFailureCategory.TRANSIENT, 1, null,
					null, null, null);
		}
		catch (RestClientException ex) {
			throw new ClaudeException("Claude API returned an unreadable response.", ex,
					ClaudeFailureCategory.UNREADABLE, 1, null, null, null, null);
		}
		if (response == null || response.content() == null) {
			throw new ClaudeException("Claude API returned no content.", null, ClaudeFailureCategory.UNREADABLE, 1,
					null, null, null, null);
		}
		return response;
	}

	private ClaudeException truncated(MessagesResponse response) {
		Integer input = (response.usage() != null) ? response.usage().inputTokens() : null;
		Integer output = (response.usage() != null) ? response.usage().outputTokens() : null;
		return new ClaudeException("Claude's reply was cut off at the max-tokens limit.", null,
				ClaudeFailureCategory.TRUNCATED, 1, null, input, output, cost(input, output));
	}

	private ClaudeCompletion toCompletion(MessagesResponse response, int attempts, long started) {
		String text = response.content()
			.stream()
			.filter((block) -> "text".equals(block.type()))
			.map(ContentBlock::text)
			.filter(Objects::nonNull)
			.collect(Collectors.joining());
		Integer input = (response.usage() != null) ? response.usage().inputTokens() : null;
		Integer output = (response.usage() != null) ? response.usage().outputTokens() : null;
		return new ClaudeCompletion(text, ClaudeCompletion.PROVIDER, this.properties.model(), attempts, input, output,
				cost(input, output), elapsedMillis(started));
	}

	private BigDecimal cost(Integer inputTokens, Integer outputTokens) {
		if (inputTokens == null || outputTokens == null) {
			return null;
		}
		BigDecimal input = BigDecimal.valueOf(inputTokens.longValue())
			.multiply(this.properties.inputUsdPerMillion())
			.divide(MILLION, 6, RoundingMode.HALF_UP);
		BigDecimal output = BigDecimal.valueOf(outputTokens.longValue())
			.multiply(this.properties.outputUsdPerMillion())
			.divide(MILLION, 6, RoundingMode.HALF_UP);
		return input.add(output);
	}

	private Duration retryAfter(RestClientResponseException ex) {
		HttpHeaders headers = ex.getResponseHeaders();
		if (headers == null) {
			return null;
		}
		String value = headers.getFirst(HttpHeaders.RETRY_AFTER);
		if (value == null || value.isBlank()) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.chars().allMatch(Character::isDigit)) {
			return Duration.ofSeconds(Long.parseLong(trimmed));
		}
		try {
			Instant until = ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
			return Duration.between(this.clock.instant(), until);
		}
		catch (DateTimeParseException ignored) {
			return null;
		}
	}

	private static ClaudeFailureCategory categoryForStatus(int status) {
		if (status == 429) {
			return ClaudeFailureCategory.RATE_LIMITED;
		}
		if (RETRYABLE_STATUSES.contains(status)) {
			return ClaudeFailureCategory.TRANSIENT;
		}
		return ClaudeFailureCategory.CLIENT_ERROR;
	}

	private void sleep(Duration delay) {
		if (delay == null || delay.isZero() || delay.isNegative()) {
			return;
		}
		try {
			this.sleeper.sleep(delay);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new ClaudeException("Claude API call was interrupted.", ex);
		}
	}

	private static void sleepQuietly(Duration duration) throws InterruptedException {
		if (duration != null && !duration.isZero() && !duration.isNegative()) {
			Thread.sleep(duration.toMillis());
		}
	}

	private static boolean isTimeout(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
				return true;
			}
		}
		return false;
	}

	private static int length(String value) {
		return (value == null) ? 0 : value.length();
	}

	private static long elapsedMillis(long startedNanos) {
		return Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000L);
	}

	private static JdkClientHttpRequestFactory requestFactory(ClaudeProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.timeout());
		return requestFactory;
	}

	record MessagesRequest(String model, @JsonProperty("max_tokens") int maxTokens, String system,
			List<Message> messages) {
	}

	record Message(String role, String content) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record MessagesResponse(List<ContentBlock> content, @JsonProperty("stop_reason") String stopReason, Usage usage) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record Usage(@JsonProperty("input_tokens") Integer inputTokens, @JsonProperty("output_tokens") Integer outputTokens) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record ContentBlock(String type, String text) {
	}

}
