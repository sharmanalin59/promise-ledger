package com.promiseledger;

import java.math.BigDecimal;
import java.util.UUID;

import com.promiseledger.company.Company;
import com.promiseledger.company.CompanyRepository;
import com.promiseledger.evidence.Evidence;
import com.promiseledger.evidence.EvidenceRepository;
import com.promiseledger.evidence.EvidenceSourceType;
import com.promiseledger.promise.Promise;
import com.promiseledger.promise.PromiseImportance;
import com.promiseledger.promise.PromiseRepository;
import com.promiseledger.promise.PromiseStatus;
import com.promiseledger.transcript.Transcript;
import com.promiseledger.transcript.TranscriptRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class PersistenceTests {

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private TranscriptRepository transcriptRepository;

	@Autowired
	private PromiseRepository promiseRepository;

	@Autowired
	private EvidenceRepository evidenceRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void persistsAndRetrievesAllFourEntities() {
		Company company = this.companyRepository.save(new Company("Test Company", "TESTCO"));
		Transcript transcript = this.transcriptRepository.save(new Transcript(company, 2026, 1, "Earnings call text"));
		Promise promise = new Promise(transcript, "Operating margin will reach 23% by Q4.", PromiseImportance.HIGH);
		promise.setCategory("margin");
		promise.setTimeframe("by Q4 FY26");
		this.promiseRepository.save(promise);
		Evidence evidence = this.evidenceRepository.save(new Evidence(promise, EvidenceSourceType.FINANCIAL_STATEMENT,
				"Q4 operating margin 23.4%", new BigDecimal("0.9500")));
		flushAndClear();

		Company loadedCompany = this.companyRepository.findById(company.getId()).orElseThrow();
		assertThat(loadedCompany.getTicker()).isEqualTo("TESTCO");
		assertThat(loadedCompany.getCreatedAt()).isNotNull();
		assertThat(loadedCompany.getUpdatedAt()).isNotNull();

		Transcript loadedTranscript = this.transcriptRepository.findById(transcript.getId()).orElseThrow();
		assertThat(loadedTranscript.getFiscalYear()).isEqualTo(2026);
		assertThat(loadedTranscript.getFiscalQuarter()).isEqualTo(1);
		assertThat(loadedTranscript.getText()).isEqualTo("Earnings call text");

		Promise loadedPromise = this.promiseRepository.findById(promise.getId()).orElseThrow();
		assertThat(loadedPromise.getStatement()).isEqualTo("Operating margin will reach 23% by Q4.");
		assertThat(loadedPromise.getImportance()).isEqualTo(PromiseImportance.HIGH);
		assertThat(loadedPromise.getStatus()).isEqualTo(PromiseStatus.OPEN);
		assertThat(loadedPromise.getTimeframe()).isEqualTo("by Q4 FY26");
		assertThat(loadedPromise.getProgressPercent()).isNull();

		Evidence loadedEvidence = this.evidenceRepository.findById(evidence.getId()).orElseThrow();
		assertThat(loadedEvidence.getSourceType()).isEqualTo(EvidenceSourceType.FINANCIAL_STATEMENT);
		assertThat(loadedEvidence.getConfidence()).isEqualByComparingTo("0.95");
		assertThat(loadedEvidence.getPromise().getId()).isEqualTo(promise.getId());
	}

	@Test
	void promiseResolvesItsCompanyThroughTranscript() {
		Company company = this.companyRepository.save(new Company("Company A", "COA"));
		Company otherCompany = this.companyRepository.save(new Company("Company B", "COB"));
		Promise promise = this.promiseRepository.save(new Promise(
				this.transcriptRepository.save(new Transcript(company, 2026, 1, "A transcript")), "A promise",
				PromiseImportance.MEDIUM));
		this.promiseRepository.save(new Promise(
				this.transcriptRepository.save(new Transcript(otherCompany, 2026, 1, "B transcript")), "B promise",
				PromiseImportance.LOW));
		flushAndClear();

		Promise loaded = this.promiseRepository.findById(promise.getId()).orElseThrow();
		assertThat(loaded.getTranscript().getCompany().getId()).isEqualTo(company.getId());
		assertThat(this.promiseRepository.findByTranscriptCompanyId(company.getId())).extracting(Promise::getId)
			.containsExactly(promise.getId());
	}

	@Test
	void rejectsSecondTranscriptForSameCompanyAndQuarter() {
		Company company = this.companyRepository.save(new Company("Test Company", "TESTCO"));
		this.transcriptRepository.saveAndFlush(new Transcript(company, 2026, 1, "First"));
		this.transcriptRepository.saveAndFlush(new Transcript(company, 2026, 2, "Next quarter"));

		assertThatThrownBy(() -> this.transcriptRepository.saveAndFlush(new Transcript(company, 2026, 1, "Duplicate")))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void allowsNullEvidenceConfidence() {
		Promise promise = savePromise();
		Evidence evidence = this.evidenceRepository
			.save(new Evidence(promise, EvidenceSourceType.EARNINGS_CALL, "Management commentary", null));
		flushAndClear();

		assertThat(this.evidenceRepository.findById(evidence.getId()).orElseThrow().getConfidence()).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = { "1.0001", "-0.0001" })
	void rejectsEvidenceConfidenceOutsideRange(String confidence) {
		Promise promise = savePromise();

		assertThatThrownBy(() -> this.evidenceRepository.saveAndFlush(
				new Evidence(promise, EvidenceSourceType.EXCHANGE_FILING, "Filing", new BigDecimal(confidence))))
			.isInstanceOf(DataIntegrityViolationException.class)
			.hasStackTraceContaining("chk_evidence_confidence");
	}

	@Test
	void rejectsUnknownPromiseStatus() {
		Promise promise = savePromise();
		this.entityManager.flush();

		assertThatThrownBy(() -> this.jdbcTemplate.update("""
				INSERT INTO promises (id, transcript_id, statement, importance, status, created_at, updated_at)
				VALUES (?, ?, 'Invalid status', 'HIGH', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
				""", UUID.randomUUID().toString(), promise.getTranscript().getId().toString()))
			.isInstanceOf(DataAccessException.class)
			.hasStackTraceContaining("chk_promises_status");
	}

	private Promise savePromise() {
		Company company = this.companyRepository.save(new Company("Test Company", "TESTCO"));
		Transcript transcript = this.transcriptRepository.save(new Transcript(company, 2026, 1, "Transcript"));
		return this.promiseRepository.save(new Promise(transcript, "Statement", PromiseImportance.MEDIUM));
	}

	private void flushAndClear() {
		this.entityManager.flush();
		this.entityManager.clear();
	}

}
