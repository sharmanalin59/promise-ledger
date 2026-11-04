package com.promiseledger.company;

import java.util.List;
import java.util.UUID;

import com.promiseledger.promise.Promise;
import com.promiseledger.promise.PromiseAssessment;
import com.promiseledger.promise.PromiseImportance;
import com.promiseledger.promise.PromiseStatus;
import com.promiseledger.transcript.Transcript;

public record CompanyLedgerResponse(UUID id, String name, String ticker, List<QuarterGroup> quarters) {

	public record QuarterGroup(int fiscalYear, int fiscalQuarter, UUID transcriptId, List<PromiseSummary> promises) {
	}

	public record PromiseSummary(UUID id, String statement, String category, PromiseImportance importance,
			String timeframe, PromiseStatus status, String assessment) {
	}

	static CompanyLedgerResponse from(Company company, List<Transcript> transcripts,
			java.util.function.Function<UUID, List<Promise>> promisesByTranscript) {
		List<QuarterGroup> quarters = transcripts.stream().map((transcript) -> {
			List<PromiseSummary> promises = promisesByTranscript.apply(transcript.getId())
				.stream()
				.map(CompanyLedgerResponse::toSummary)
				.toList();
			return new QuarterGroup(transcript.getFiscalYear(), transcript.getFiscalQuarter(), transcript.getId(),
					promises);
		}).toList();
		return new CompanyLedgerResponse(company.getId(), company.getName(), company.getTicker(), quarters);
	}

	private static PromiseSummary toSummary(Promise promise) {
		return new PromiseSummary(promise.getId(), promise.getStatement(), promise.getCategory(),
				promise.getImportance(), promise.getTimeframe(), promise.getStatus(),
				PromiseAssessment.labelFor(promise.getStatus()));
	}

}
