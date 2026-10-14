package com.promiseledger.transcript;

import java.time.Instant;
import java.util.UUID;

public record TranscriptResponse(UUID id, String companyTicker, String companyName, int fiscalYear,
		int fiscalQuarter, int textLength, Instant createdAt) {

	static TranscriptResponse from(Transcript transcript) {
		return new TranscriptResponse(transcript.getId(), transcript.getCompany().getTicker(),
				transcript.getCompany().getName(), transcript.getFiscalYear(), transcript.getFiscalQuarter(),
				transcript.getText().length(), transcript.getCreatedAt());
	}

}
