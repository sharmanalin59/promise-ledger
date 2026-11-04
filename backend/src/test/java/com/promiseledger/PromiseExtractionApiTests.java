package com.promiseledger;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.promiseledger.company.CompanyRepository;
import com.promiseledger.extraction.ClaudeCircuitOpenException;
import com.promiseledger.extraction.ClaudeClient;
import com.promiseledger.extraction.ClaudeCompletion;
import com.promiseledger.extraction.ClaudeException;
import com.promiseledger.extraction.ClaudeFailureCategory;
import com.promiseledger.extraction.ClaudeInputTooLargeException;
import com.promiseledger.extraction.ClaudeTimeoutException;
import com.promiseledger.extraction.ExtractionRunRepository;
import com.promiseledger.extraction.ExtractionRunStatus;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PromiseExtractionApiTests {

	private static final String TRANSCRIPT_TEXT = """
			Good morning, everyone. Revenue grew 12% this quarter.
			We expect revenue growth of 15% in FY27, driven by our automotive clients.
			We plan to hire 2,000 engineers
			by March 2027. Thank you.
			""";

	private static final String VALID_OUTPUT = """
			{"promises": [
			  {"statement": "Revenue will grow 15% in FY27.",
			   "sourceQuote": "We expect revenue growth of 15% in FY27, driven by our automotive clients.",
			   "category": "revenue", "importance": "HIGH", "timeframe": "FY27"},
			  {"statement": "The company will hire 2,000 engineers by March 2027.",
			   "sourceQuote": "we plan to hire 2,000 engineers by March 2027",
			   "category": "hiring", "importance": "MEDIUM", "timeframe": null}
			]}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CompanyRepository companyRepository;

	@Autowired
	private TranscriptRepository transcriptRepository;

	@Autowired
	private PromiseRepository promiseRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ExtractionRunRepository extractionRunRepository;

	@MockitoBean
	private ClaudeClient claudeClient;

	private UUID transcriptId;

	@BeforeEach
	void createTranscript() {
		given(this.claudeClient.isConfigured()).willReturn(true);
		Transcript transcript = new Transcript(this.companyRepository.findByTicker("KPITTECH").orElseThrow(), 2026, 2,
				TRANSCRIPT_TEXT);
		this.transcriptId = this.transcriptRepository.save(transcript).getId();
	}

	@AfterEach
	void deleteTestData() {
		this.jdbcTemplate.update("DELETE FROM evidence");
		this.jdbcTemplate.update("DELETE FROM promises");
		this.jdbcTemplate.update("DELETE FROM extraction_runs");
		this.jdbcTemplate.update("DELETE FROM transcripts");
	}

	@Test
	void savesValidatedPromisesAndReturnsThem() throws Exception {
		given(this.claudeClient.completeDetailed(anyString(), anyString())).willReturn(reply(VALID_OUTPUT));

		extract().andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.transcriptId").value(this.transcriptId.toString()))
			.andExpect(jsonPath("$.promiseCount").value(2))
			.andExpect(jsonPath("$.promises[0].id").isNotEmpty())
			.andExpect(jsonPath("$.promises[0].statement").value("Revenue will grow 15% in FY27."))
			.andExpect(jsonPath("$.promises[0].sourceQuote")
				.value("We expect revenue growth of 15% in FY27, driven by our automotive clients."))
			.andExpect(jsonPath("$.promises[0].category").value("revenue"))
			.andExpect(jsonPath("$.promises[0].importance").value("HIGH"))
			.andExpect(jsonPath("$.promises[0].timeframe").value("FY27"))
			.andExpect(jsonPath("$.promises[0].status").value("OPEN"))
			.andExpect(jsonPath("$.promises[1].importance").value("MEDIUM"))
			.andExpect(jsonPath("$.promises[1].timeframe").doesNotExist());

		verify(this.claudeClient).completeDetailed(contains("management promises"), contains(TRANSCRIPT_TEXT));
		List<Promise> saved = savedPromises();
		assertThat(saved).hasSize(2);
		assertThat(saved).extracting(Promise::getStatus).containsOnly(PromiseStatus.OPEN);
		assertThat(saved).extracting(Promise::getImportance)
			.containsExactlyInAnyOrder(PromiseImportance.HIGH, PromiseImportance.MEDIUM);
		assertThat(saved).extracting(Promise::getSourceQuote)
			.contains("we plan to hire 2,000 engineers by March 2027");
	}

	@Test
	void acceptsJsonWrappedInMarkdownFence() throws Exception {
		given(this.claudeClient.completeDetailed(anyString(), anyString())).willReturn(reply("```json\n" + VALID_OUTPUT + "\n```"));

		extract().andExpect(status().isOk()).andExpect(jsonPath("$.promiseCount").value(2));
		assertThat(savedPromises()).hasSize(2);
	}

	@Test
	void emptyPromiseListIsASuccessfulExtractionWithNothingSaved() throws Exception {
		given(this.claudeClient.completeDetailed(anyString(), anyString())).willReturn(reply("{\"promises\": []}"));

		extract().andExpect(status().isOk())
			.andExpect(jsonPath("$.promiseCount").value(0))
			.andExpect(jsonPath("$.promises", hasSize(0)));
		assertThat(savedPromises()).isEmpty();
	}

	@Test
	void rejectsEmptyOutput() throws Exception {
		given(this.claudeClient.completeDetailed(anyString(), anyString())).willReturn(reply("   "));

		expectFailure(502, "Claude returned an empty response. No promises were saved.");
	}

	@Test
	void rejectsMalformedJson() throws Exception {
		given(this.claudeClient.completeDetailed(anyString(), anyString())).willReturn(reply("{\"promises\": [{\"statement\": "));

		expectFailure(502, "Claude returned malformed JSON. No promises were saved.");
	}

	@Test
	void rejectsOutputWithoutPromisesList() throws Exception {
		given(this.claudeClient.completeDetailed(anyString(), anyString())).willReturn(reply("{\"items\": []}"));

		expectFailure(502, "Claude returned promises that failed validation. No promises were saved.")
			.andExpect(jsonPath("$.errors", hasSize(1)))
			.andExpect(jsonPath("$.errors[0]").value("promises: is required"));
	}

	@Test
	void rejectsWholeOutputWhenAnyPromiseHasInvalidFields() throws Exception {
		given(this.claudeClient.completeDetailed(anyString(), anyString())).willReturn(reply("""
				{"promises": [
				  {"statement": "Revenue will grow 15% in FY27.",
				   "sourceQuote": "We expect revenue growth of 15% in FY27", "importance": "HIGH"},
				  {"statement": " ", "sourceQuote": "We plan to hire 2,000 engineers", "importance": "CRITICAL",
				   "category": "LONG_CATEGORY"}
				]}
				""".replace("LONG_CATEGORY", "x".repeat(101))));

		expectFailure(502, "Claude returned promises that failed validation. No promises were saved.")
			.andExpect(jsonPath("$.errors", hasSize(3)))
			.andExpect(jsonPath("$.errors[0]").value("promises[1].category: must be at most 100 characters"))
			.andExpect(jsonPath("$.errors[1]").value("promises[1].importance: must be HIGH, MEDIUM, or LOW"))
			.andExpect(jsonPath("$.errors[2]").value("promises[1].statement: is required"));
	}

	@Test
	void rejectsQuotesThatAreNotInTheTranscript() throws Exception {
		given(this.claudeClient.completeDetailed(anyString(), anyString())).willReturn(reply("""
				{"promises": [
				  {"statement": "Revenue will grow 15% in FY27.",
				   "sourceQuote": "We expect revenue growth of 15% in FY27", "importance": "HIGH"},
				  {"statement": "Margins will reach 20%.",
				   "sourceQuote": "We will reach a 20% operating margin next year.", "importance": "HIGH"}
				]}
				"""));

		expectFailure(502, "Claude returned promises that failed validation. No promises were saved.")
			.andExpect(jsonPath("$.errors", hasSize(1)))
			.andExpect(jsonPath("$.errors[0]").value("promises[1].sourceQuote: was not found in the transcript"));
	}

	@Test
	void reportsTimeoutAsGatewayTimeout() throws Exception {
		given(this.claudeClient.completeDetailed(anyString(), anyString())).willThrow(
				new ClaudeTimeoutException("Claude API did not respond within 120 seconds.", null));

		expectFailure(504, "Claude API did not respond within 120 seconds. No promises were saved.");
	}

	@Test
	void reportsProviderErrorAsBadGateway() throws Exception {
		given(this.claudeClient.completeDetailed(anyString(), anyString()))
			.willThrow(new ClaudeException("Claude API returned HTTP 529."));

		expectFailure(502, "Claude API returned HTTP 529. No promises were saved.");
	}

	@Test
	void reportsMissingApiKeyWithoutCallingClaude() throws Exception {
		given(this.claudeClient.isConfigured()).willReturn(false);

		expectFailure(503, "Promise extraction is unavailable: the Claude API key is not configured.");
		verify(this.claudeClient, never()).completeDetailed(anyString(), anyString());
		assertThat(this.extractionRunRepository.findByTranscriptIdOrderByCreatedAtDesc(this.transcriptId))
			.singleElement()
			.satisfies((run) -> {
				assertThat(run.getStatus()).isEqualTo(ExtractionRunStatus.FAILED);
				assertThat(run.getFailureCategory()).isEqualTo(ClaudeFailureCategory.NOT_CONFIGURED);
				assertThat(run.getAttemptCount()).isZero();
			});
	}

	@Test
	void reportsPersistenceFailureAndSavesNothing() throws Exception {
		given(this.claudeClient.completeDetailed(anyString(), anyString())).willAnswer((invocation) -> {
			this.jdbcTemplate.update("DELETE FROM transcripts WHERE id = ?", this.transcriptId.toString());
			return reply(VALID_OUTPUT);
		});

		extract().andExpect(status().isInternalServerError())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Extracted promises could not be saved. No promises were saved."));
		assertThat(this.promiseRepository.count()).isZero();
	}

	@Test
	void rejectsUnknownTranscript() throws Exception {
		this.mockMvc.perform(post("/api/transcripts/{id}/extract", UUID.randomUUID()).with(user("demo")).with(csrf()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.detail", containsString("was not found")));
		verify(this.claudeClient, never()).completeDetailed(anyString(), anyString());
	}

	@Test
	void rejectsSecondExtractionForSameTranscript() throws Exception {
		given(this.claudeClient.completeDetailed(anyString(), anyString())).willReturn(reply(VALID_OUTPUT));
		extract().andExpect(status().isOk());

		extract().andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail")
				.value("Promises have already been extracted for transcript " + this.transcriptId + "."));
		verify(this.claudeClient, times(1)).completeDetailed(anyString(), anyString());
		assertThat(savedPromises()).hasSize(2);
	}

	@Test
	void returnsTokenUsageAndStoresItForInspection() throws Exception {
		given(this.claudeClient.completeDetailed(anyString(), anyString())).willReturn(new ClaudeCompletion(VALID_OUTPUT,
				"anthropic", "claude-test-model", 2, 1000, 500, new BigDecimal("0.010500"), 12));

		extract().andExpect(status().isOk())
			.andExpect(jsonPath("$.usage.provider").value("anthropic"))
			.andExpect(jsonPath("$.usage.model").value("claude-test-model"))
			.andExpect(jsonPath("$.usage.attempts").value(2))
			.andExpect(jsonPath("$.usage.inputTokens").value(1000))
			.andExpect(jsonPath("$.usage.outputTokens").value(500))
			.andExpect(jsonPath("$.usage.estimatedCostUsd").value(0.0105));

		this.mockMvc.perform(get("/api/transcripts/{id}/extractions", this.transcriptId).with(user("demo")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].status").value("SUCCESS"))
			.andExpect(jsonPath("$[0].attemptCount").value(2))
			.andExpect(jsonPath("$[0].inputTokens").value(1000))
			.andExpect(jsonPath("$[0].estimatedCostUsd").value(0.0105))
			.andExpect(jsonPath("$[0].failureCategory").value(nullValue()));
	}

	@Test
	void rejectsOversizedInputBeforeSavingPromises() throws Exception {
		given(this.claudeClient.completeDetailed(anyString(), anyString()))
			.willThrow(new ClaudeInputTooLargeException(200_000, 100_000));

		expectFailure(400,
				"Transcript is too large to send to Claude (estimated 200000 tokens, limit 100000). No promises were saved.");
		assertThat(this.extractionRunRepository.findByTranscriptIdOrderByCreatedAtDesc(this.transcriptId))
			.singleElement()
			.satisfies((run) -> {
				assertThat(run.getFailureCategory()).isEqualTo(ClaudeFailureCategory.INPUT_TOO_LARGE);
				assertThat(run.getAttemptCount()).isZero();
				assertThat(run.getStatus()).isEqualTo(ExtractionRunStatus.FAILED);
			});
	}

	@Test
	void reportsOpenCircuitWithoutCallingThrough() throws Exception {
		given(this.claudeClient.completeDetailed(anyString(), anyString()))
			.willThrow(new ClaudeCircuitOpenException());

		expectFailure(503, "Claude API is temporarily unavailable after repeated failures. No promises were saved.");
		assertThat(this.extractionRunRepository.findByTranscriptIdOrderByCreatedAtDesc(this.transcriptId))
			.singleElement()
			.extracting((run) -> run.getFailureCategory())
			.isEqualTo(ClaudeFailureCategory.CIRCUIT_OPEN);
	}

	private ResultActions extract() throws Exception {
		return this.mockMvc
			.perform(post("/api/transcripts/{id}/extract", this.transcriptId).with(user("demo")).with(csrf()));
	}

	private ResultActions expectFailure(int status, String detail) throws Exception {
		ResultActions result = extract().andExpect(status().is(status))
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value(detail));
		assertThat(savedPromises()).isEmpty();
		assertThat(this.transcriptRepository.existsById(this.transcriptId)).isTrue();
		return result;
	}

	private static ClaudeCompletion reply(String text) {
		return new ClaudeCompletion(text, "anthropic", "claude-test-model", 1, null, null, null, 1);
	}

	private List<Promise> savedPromises() {
		return this.promiseRepository.findAll()
			.stream()
			.filter((promise) -> promise.getTranscript().getId().equals(this.transcriptId))
			.toList();
	}

}
