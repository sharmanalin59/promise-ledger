package com.promiseledger.extraction;

import java.math.BigDecimal;
import java.time.Duration;

public class ClaudeException extends RuntimeException {

	private final ClaudeFailureCategory category;

	private final int attempts;

	private final Duration retryAfter;

	private final Integer inputTokens;

	private final Integer outputTokens;

	private final BigDecimal estimatedCostUsd;

	public ClaudeException(String message) {
		this(message, null, ClaudeFailureCategory.CLIENT_ERROR, 1, null, null, null, null);
	}

	public ClaudeException(String message, Throwable cause) {
		this(message, cause, ClaudeFailureCategory.CLIENT_ERROR, 1, null, null, null, null);
	}

	ClaudeException(String message, Throwable cause, ClaudeFailureCategory category, int attempts, Duration retryAfter,
			Integer inputTokens, Integer outputTokens, BigDecimal estimatedCostUsd) {
		super(message, cause);
		this.category = category;
		this.attempts = attempts;
		this.retryAfter = retryAfter;
		this.inputTokens = inputTokens;
		this.outputTokens = outputTokens;
		this.estimatedCostUsd = estimatedCostUsd;
	}

	public ClaudeFailureCategory category() {
		return this.category;
	}

	public int attempts() {
		return this.attempts;
	}

	Duration retryAfter() {
		return this.retryAfter;
	}

	public Integer inputTokens() {
		return this.inputTokens;
	}

	public Integer outputTokens() {
		return this.outputTokens;
	}

	public BigDecimal estimatedCostUsd() {
		return this.estimatedCostUsd;
	}

	public ClaudeException withAttempts(int attempts) {
		return new ClaudeException(getMessage(), getCause(), this.category, attempts, this.retryAfter, this.inputTokens,
				this.outputTokens, this.estimatedCostUsd);
	}

}
