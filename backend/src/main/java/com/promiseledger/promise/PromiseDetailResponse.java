package com.promiseledger.promise;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.promiseledger.company.Company;
import com.promiseledger.evidence.Evidence;
import com.promiseledger.evidence.EvidenceSourceType;
import com.promiseledger.transcript.Transcript;

public record PromiseDetailResponse(UUID id, String statement, String sourceQuote, String category,
		PromiseImportance importance, String timeframe, PromiseStatus status, String assessment, String speaker,
		CompanyRef company, TranscriptRef transcript, List<EvidenceItem> evidence) {

	public record CompanyRef(UUID id, String name, String ticker) {
	}

	public record TranscriptRef(UUID id, int fiscalYear, int fiscalQuarter) {
	}

	public record EvidenceItem(UUID id, EvidenceSourceType sourceType, String text, BigDecimal confidence) {
	}

	static PromiseDetailResponse from(Promise promise, List<Evidence> evidence) {
		Transcript transcript = promise.getTranscript();
		Company company = transcript.getCompany();
		return new PromiseDetailResponse(promise.getId(), promise.getStatement(), promise.getSourceQuote(),
				promise.getCategory(), promise.getImportance(), promise.getTimeframe(), promise.getStatus(),
				PromiseAssessment.labelFor(promise.getStatus()), null,
				new CompanyRef(company.getId(), company.getName(), company.getTicker()),
				new TranscriptRef(transcript.getId(), transcript.getFiscalYear(), transcript.getFiscalQuarter()),
				evidence.stream()
					.map((item) -> new EvidenceItem(item.getId(), item.getSourceType(), item.getText(),
							item.getConfidence()))
					.toList());
	}

}
