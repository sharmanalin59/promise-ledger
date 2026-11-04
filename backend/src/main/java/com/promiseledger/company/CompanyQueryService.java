package com.promiseledger.company;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.promiseledger.promise.PromiseRepository;
import com.promiseledger.transcript.Transcript;
import com.promiseledger.transcript.TranscriptRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;

@Service
public class CompanyQueryService {

	private final CompanyRepository companyRepository;

	private final TranscriptRepository transcriptRepository;

	private final PromiseRepository promiseRepository;

	public CompanyQueryService(CompanyRepository companyRepository, TranscriptRepository transcriptRepository,
			PromiseRepository promiseRepository) {
		this.companyRepository = companyRepository;
		this.transcriptRepository = transcriptRepository;
		this.promiseRepository = promiseRepository;
	}

	@Transactional(readOnly = true)
	public List<CompanyResponse> listCompanies() {
		return this.companyRepository.findAll()
			.stream()
			.sorted(Comparator.comparing(Company::getName, String.CASE_INSENSITIVE_ORDER))
			.map(CompanyResponse::from)
			.toList();
	}

	@Transactional(readOnly = true)
	public CompanyLedgerResponse getLedger(UUID companyId) {
		Company company = this.companyRepository.findById(companyId)
			.orElseThrow(() -> new ErrorResponseException(HttpStatus.NOT_FOUND,
					ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Company " + companyId + " was not found."),
					null));
		List<Transcript> transcripts = this.transcriptRepository
			.findByCompanyIdOrderByFiscalYearDescFiscalQuarterDesc(companyId);
		return CompanyLedgerResponse.from(company, transcripts,
				(transcriptId) -> this.promiseRepository.findByTranscriptIdOrderByCreatedAtAsc(transcriptId));
	}

}
