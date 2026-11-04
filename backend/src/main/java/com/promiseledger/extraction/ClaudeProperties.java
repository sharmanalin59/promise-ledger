package com.promiseledger.extraction;

import java.math.BigDecimal;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("claude")
public record ClaudeProperties(String apiKey, String model, String baseUrl, Duration connectTimeout, Duration timeout,
		int maxTokens, int maxAttempts, Duration retryInitialBackoff, Duration retryMaxBackoff,
		int circuitFailureThreshold, Duration circuitOpenDuration, int maxConcurrentCalls, Duration concurrencyWait,
		int maxInputTokens, BigDecimal inputUsdPerMillion, BigDecimal outputUsdPerMillion) {

	public ClaudeProperties {
		requirePositive(connectTimeout, "claude.connect-timeout");
		requirePositive(timeout, "claude.timeout");
		requirePositive(retryInitialBackoff, "claude.retry-initial-backoff");
		requirePositive(retryMaxBackoff, "claude.retry-max-backoff");
		requirePositive(circuitOpenDuration, "claude.circuit-open-duration");
		if (concurrencyWait == null || concurrencyWait.isNegative()) {
			throw new IllegalArgumentException("claude.concurrency-wait must be zero or positive");
		}
		if (retryMaxBackoff.compareTo(retryInitialBackoff) < 0) {
			throw new IllegalArgumentException("claude.retry-max-backoff must be at least claude.retry-initial-backoff");
		}
		if (maxTokens < 1) {
			throw new IllegalArgumentException("claude.max-tokens must be at least 1");
		}
		if (maxAttempts < 1) {
			throw new IllegalArgumentException("claude.max-attempts must be at least 1");
		}
		if (circuitFailureThreshold < 1) {
			throw new IllegalArgumentException("claude.circuit-failure-threshold must be at least 1");
		}
		if (maxConcurrentCalls < 1) {
			throw new IllegalArgumentException("claude.max-concurrent-calls must be at least 1");
		}
		if (maxInputTokens < 1) {
			throw new IllegalArgumentException("claude.max-input-tokens must be at least 1");
		}
		if (inputUsdPerMillion == null || inputUsdPerMillion.signum() < 0) {
			throw new IllegalArgumentException("claude.input-usd-per-million must be zero or positive");
		}
		if (outputUsdPerMillion == null || outputUsdPerMillion.signum() < 0) {
			throw new IllegalArgumentException("claude.output-usd-per-million must be zero or positive");
		}
	}

	/**
	 * Settings that keep a single attempt and a high circuit threshold, for tests of one HTTP call.
	 */
	static ClaudeProperties forTests(String apiKey, String model, String baseUrl, Duration timeout, int maxTokens) {
		return new ClaudeProperties(apiKey, model, baseUrl, Duration.ofSeconds(10), timeout, maxTokens, 1,
				Duration.ofMillis(1), Duration.ofSeconds(30), 100, Duration.ofMinutes(5), 4, Duration.ofSeconds(1),
				1_000_000, new BigDecimal("3"), new BigDecimal("15"));
	}

	public boolean hasApiKey() {
		return this.apiKey != null && !this.apiKey.isBlank();
	}

	@Override
	public String toString() {
		return "ClaudeProperties[apiKey=" + (hasApiKey() ? "****" : "<not set>") + ", model=" + this.model
				+ ", baseUrl=" + this.baseUrl + ", connectTimeout=" + this.connectTimeout + ", timeout=" + this.timeout
				+ ", maxTokens=" + this.maxTokens + ", maxAttempts=" + this.maxAttempts + ", maxConcurrentCalls="
				+ this.maxConcurrentCalls + ", maxInputTokens=" + this.maxInputTokens + "]";
	}

	private static void requirePositive(Duration duration, String name) {
		if (duration == null || duration.isZero() || duration.isNegative()) {
			throw new IllegalArgumentException(name + " must be positive");
		}
	}

}
