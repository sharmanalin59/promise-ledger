package com.promiseledger;

import java.util.List;

import com.promiseledger.company.CompanyRepository;
import com.promiseledger.extraction.PromiseExtractionService;
import com.promiseledger.promise.Promise;
import com.promiseledger.promise.PromiseStatus;
import com.promiseledger.transcript.Transcript;
import com.promiseledger.transcript.TranscriptRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Calls the real Claude API and incurs API charges. Runs only when
 * {@code CLAUDE_LIVE_TEST=true} and {@code ANTHROPIC_API_KEY} are set.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "CLAUDE_LIVE_TEST", matches = "true")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
class ClaudeLiveExtractionTests {

	private static final String TRANSCRIPT_TEXT = """
			Operator: Welcome to the Q2 FY27 earnings call.
			CEO: Revenue grew 11% year on year this quarter. For the full year FY27 we expect revenue growth
			between 14% and 16%, and we remain confident of reaching an EBITDA margin of 21% by Q4 FY27.
			We will also open a new delivery centre in Pune before the end of this fiscal year.
			Analyst: Thank you. Could you comment on hiring?
			CFO: We added 1,200 people this quarter, and we plan to add another 3,000 engineers in FY28.
			""";

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private TranscriptRepository transcriptRepository;

	@Autowired
	private PromiseExtractionService extractionService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void deleteTestData() {
		this.jdbcTemplate.update("DELETE FROM evidence");
		this.jdbcTemplate.update("DELETE FROM promises");
		this.jdbcTemplate.update("DELETE FROM transcripts");
	}

	@Test
	void extractsGroundedPromisesFromRealClaude() {
		Transcript transcript = this.transcriptRepository.save(new Transcript(
				this.companyRepository.findByTicker("PERSISTENT").orElseThrow(), 2027, 2, TRANSCRIPT_TEXT));

		List<Promise> promises = this.extractionService.extract(transcript.getId());

		assertThat(promises).isNotEmpty();
		assertThat(promises).allSatisfy((promise) -> {
			assertThat(promise.getStatus()).isEqualTo(PromiseStatus.OPEN);
			assertThat(promise.getStatement()).isNotBlank();
			assertThat(TRANSCRIPT_TEXT.replaceAll("\\s+", " ").toLowerCase())
				.contains(promise.getSourceQuote().replaceAll("\\s+", " ").toLowerCase());
		});
	}

}
