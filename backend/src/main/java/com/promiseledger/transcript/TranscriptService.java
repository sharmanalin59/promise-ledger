package com.promiseledger.transcript;

import java.util.Locale;
import java.util.stream.Collectors;

import com.promiseledger.company.Company;
import com.promiseledger.company.CompanyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TranscriptService {

	private final CompanyRepository companyRepository;

	private final TranscriptRepository transcriptRepository;

	public TranscriptService(CompanyRepository companyRepository, TranscriptRepository transcriptRepository) {
		this.companyRepository = companyRepository;
		this.transcriptRepository = transcriptRepository;
	}

	@Transactional
	public Transcript submit(SubmitTranscriptRequest request) {
		String ticker = request.companyTicker().trim().toUpperCase(Locale.ROOT);
		Company company = this.companyRepository.findByTicker(ticker)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Unknown companyTicker '%s'. Supported tickers: %s.".formatted(ticker, supportedTickers())));
		if (this.transcriptRepository.existsByCompanyAndFiscalYearAndFiscalQuarter(company, request.fiscalYear(),
				request.fiscalQuarter())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "A transcript for %s FY%d Q%d already exists."
				.formatted(ticker, request.fiscalYear(), request.fiscalQuarter()));
		}
		return this.transcriptRepository
			.saveAndFlush(new Transcript(company, request.fiscalYear(), request.fiscalQuarter(), request.text()));
	}

	private String supportedTickers() {
		return this.companyRepository.findAll()
			.stream()
			.map(Company::getTicker)
			.sorted()
			.collect(Collectors.joining(", "));
	}

}
