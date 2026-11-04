package com.promiseledger;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CompanyLedgerApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private TranscriptRepository transcriptRepository;

	@Autowired
	private PromiseRepository promiseRepository;

	@Autowired
	private EvidenceRepository evidenceRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Company company;

	private UUID promiseId;

	@BeforeEach
	void seed() {
		this.company = this.companyRepository.findByTicker("KPITTECH").orElseThrow();
		Transcript q2 = this.transcriptRepository
			.save(new Transcript(this.company, 2026, 2, "Q2 call. We expect margins of 20% by FY27."));
		// Q1 transcript with zero promises — must appear as empty, not as a miss.
		this.transcriptRepository.save(new Transcript(this.company, 2026, 1, "Q1 call with no promises."));
		Promise open = new Promise(q2, "Margins will reach 20% by FY27.", PromiseImportance.HIGH);
		open.setSourceQuote("We expect margins of 20% by FY27.");
		open.setTimeframe("FY27");
		open.setCategory("margin");
		this.promiseId = this.promiseRepository.save(open).getId();

		Promise delivered = new Promise(q2, "Revenue grew as guided.", PromiseImportance.MEDIUM);
		delivered.setStatus(PromiseStatus.DELIVERED);
		delivered = this.promiseRepository.save(delivered);
		this.evidenceRepository.save(new Evidence(delivered, EvidenceSourceType.FINANCIAL_STATEMENT,
				"Annual report confirms guidance met.", null));
	}

	@AfterEach
	void cleanup() {
		this.jdbcTemplate.update("DELETE FROM evidence");
		this.jdbcTemplate.update("DELETE FROM promises");
		this.jdbcTemplate.update("DELETE FROM transcripts");
	}

	@Test
	void listsCompaniesInSharedCatalogue() throws Exception {
		this.mockMvc.perform(get("/api/companies").with(user("demo")))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$", hasSize(5)))
			.andExpect(jsonPath("$[0].name").isNotEmpty())
			.andExpect(jsonPath("$[0].ticker").isNotEmpty());
	}

	@Test
	void rejectsUnauthenticatedCompanyList() throws Exception {
		this.mockMvc.perform(get("/api/companies")).andExpect(status().isUnauthorized());
	}

	@Test
	void companyLedgerGroupsPromisesByQuarterNewestFirst() throws Exception {
		this.mockMvc.perform(get("/api/companies/{id}", this.company.getId()).with(user("demo")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.ticker").value("KPITTECH"))
			.andExpect(jsonPath("$.quarters", hasSize(2)))
			.andExpect(jsonPath("$.quarters[0].fiscalYear").value(2026))
			.andExpect(jsonPath("$.quarters[0].fiscalQuarter").value(2))
			.andExpect(jsonPath("$.quarters[0].promises", hasSize(2)))
			.andExpect(jsonPath("$.quarters[0].promises[0].assessment").value("Unassessed"))
			.andExpect(jsonPath("$.quarters[0].promises[1].assessment").value("Delivered"))
			.andExpect(jsonPath("$.quarters[1].fiscalQuarter").value(1))
			.andExpect(jsonPath("$.quarters[1].promises", hasSize(0)));
	}

	@Test
	void unknownCompanyReturns404() throws Exception {
		this.mockMvc.perform(get("/api/companies/{id}", UUID.randomUUID()).with(user("demo")))
			.andExpect(status().isNotFound());
	}

	@Test
	void promiseDetailIncludesSourceQuoteAndEvidence() throws Exception {
		this.mockMvc.perform(get("/api/promises/{id}", this.promiseId).with(user("demo")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.statement").value("Margins will reach 20% by FY27."))
			.andExpect(jsonPath("$.sourceQuote").value("We expect margins of 20% by FY27."))
			.andExpect(jsonPath("$.assessment").value("Unassessed"))
			.andExpect(jsonPath("$.speaker").value(org.hamcrest.Matchers.nullValue()))
			.andExpect(jsonPath("$.timeframe").value("FY27"))
			.andExpect(jsonPath("$.company.ticker").value("KPITTECH"))
			.andExpect(jsonPath("$.transcript.fiscalQuarter").value(2))
			.andExpect(jsonPath("$.evidence", hasSize(0)));
	}

	@Test
	void promiseDetailIncludesEvidenceWhenPresent() throws Exception {
		Promise delivered = this.promiseRepository.findAll()
			.stream()
			.filter((promise) -> promise.getStatus() == PromiseStatus.DELIVERED)
			.findFirst()
			.orElseThrow();

		this.mockMvc.perform(get("/api/promises/{id}", delivered.getId()).with(user("demo")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.assessment").value("Delivered"))
			.andExpect(jsonPath("$.evidence", hasSize(1)))
			.andExpect(jsonPath("$.evidence[0].sourceType").value("FINANCIAL_STATEMENT"));
	}

}
