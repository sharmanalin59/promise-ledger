package com.promiseledger.extraction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ExtractionRunResponse(UUID id, UUID transcriptId, String provider, String model, int attemptCount,
		Integer inputTokens, Integer outputTokens, BigDecimal estimatedCostUsd, long latencyMillis,
		ClaudeFailureCategory failureCategory, ExtractionRunStatus status, Instant createdAt) {

	static ExtractionRunResponse from(ExtractionRun run) {
		return new ExtractionRunResponse(run.getId(), run.getTranscript().getId(), run.getProvider(), run.getModel(),
				run.getAttemptCount(), run.getInputTokens(), run.getOutputTokens(), run.getEstimatedCostUsd(),
				run.getLatencyMillis(), run.getFailureCategory(), run.getStatus(), run.getCreatedAt());
	}

}
