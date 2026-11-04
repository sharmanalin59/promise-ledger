package com.promiseledger.extraction;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.promiseledger.promise.Promise;
import com.promiseledger.promise.PromiseImportance;
import com.promiseledger.promise.PromiseStatus;

public record ExtractionResponse(UUID transcriptId, int promiseCount, List<ExtractedPromiseResponse> promises,
		ExtractionUsage usage) {

	static ExtractionResponse from(UUID transcriptId, ExtractionOutcome outcome) {
		return new ExtractionResponse(transcriptId, outcome.promises().size(),
				outcome.promises().stream().map(ExtractedPromiseResponse::from).toList(), outcome.usage());
	}

	public record ExtractedPromiseResponse(UUID id, String statement, String sourceQuote, String category,
			PromiseImportance importance, String timeframe, PromiseStatus status) {

		static ExtractedPromiseResponse from(Promise promise) {
			return new ExtractedPromiseResponse(promise.getId(), promise.getStatement(), promise.getSourceQuote(),
					promise.getCategory(), promise.getImportance(), promise.getTimeframe(), promise.getStatus());
		}

	}

	public record ExtractionUsage(String provider, String model, int attempts, Integer inputTokens,
			Integer outputTokens, BigDecimal estimatedCostUsd, long latencyMillis) {
	}

}
