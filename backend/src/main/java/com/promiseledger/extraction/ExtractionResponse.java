package com.promiseledger.extraction;

import java.util.List;
import java.util.UUID;

import com.promiseledger.promise.Promise;
import com.promiseledger.promise.PromiseImportance;
import com.promiseledger.promise.PromiseStatus;

public record ExtractionResponse(UUID transcriptId, int promiseCount, List<ExtractedPromiseResponse> promises) {

	static ExtractionResponse from(UUID transcriptId, List<Promise> promises) {
		return new ExtractionResponse(transcriptId, promises.size(),
				promises.stream().map(ExtractedPromiseResponse::from).toList());
	}

	public record ExtractedPromiseResponse(UUID id, String statement, String sourceQuote, String category,
			PromiseImportance importance, String timeframe, PromiseStatus status) {

		static ExtractedPromiseResponse from(Promise promise) {
			return new ExtractedPromiseResponse(promise.getId(), promise.getStatement(), promise.getSourceQuote(),
					promise.getCategory(), promise.getImportance(), promise.getTimeframe(), promise.getStatus());
		}

	}

}
