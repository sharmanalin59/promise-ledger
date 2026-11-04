package com.promiseledger.extraction;

import java.math.BigDecimal;

public record ClaudeCompletion(String text, String provider, String model, int attempts, Integer inputTokens,
		Integer outputTokens, BigDecimal estimatedCostUsd, long latencyMillis) {

	static final String PROVIDER = "anthropic";

}
