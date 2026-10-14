package com.promiseledger;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import com.promiseledger.transcript.SubmitTranscriptRequest;
import com.promiseledger.transcript.Transcript;
import com.promiseledger.transcript.TranscriptRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TranscriptApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TranscriptRepository transcriptRepository;

	@Test
	void storesTranscriptAndReturnsItsDetails() throws Exception {
		String response = submit(request("kpittech", 2026, 1, "We expect revenue growth of 15% next year."))
			.andExpect(status().isCreated())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.*", hasSize(7)))
			.andExpect(jsonPath("$.id").isNotEmpty())
			.andExpect(jsonPath("$.companyTicker").value("KPITTECH"))
			.andExpect(jsonPath("$.companyName").value("KPIT Technologies"))
			.andExpect(jsonPath("$.fiscalYear").value(2026))
			.andExpect(jsonPath("$.fiscalQuarter").value(1))
			.andExpect(jsonPath("$.textLength").value(42))
			.andExpect(jsonPath("$.createdAt").isNotEmpty())
			.andReturn()
			.getResponse()
			.getContentAsString();

		UUID id = UUID.fromString(JsonPath.read(response, "$.id"));
		Transcript stored = this.transcriptRepository.findById(id).orElseThrow();
		assertThat(stored.getText()).isEqualTo("We expect revenue growth of 15% next year.");
		assertThat(stored.getCompany().getTicker()).isEqualTo("KPITTECH");
	}

	@Test
	void acceptsTextAtMaximumLength() throws Exception {
		submit(request("COFORGE", 2026, 2, "a".repeat(SubmitTranscriptRequest.MAX_TEXT_LENGTH)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.textLength").value(SubmitTranscriptRequest.MAX_TEXT_LENGTH));
	}

	@Test
	void rejectsMissingAndBlankFields() throws Exception {
		submit("""
				{"companyTicker": " ", "text": "   "}
				""")
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("One or more fields are invalid."))
			.andExpect(jsonPath("$.errors", hasSize(4)))
			.andExpect(jsonPath("$.errors[0].field").value("companyTicker"))
			.andExpect(jsonPath("$.errors[0].message").value("is required"))
			.andExpect(jsonPath("$.errors[1].field").value("fiscalQuarter"))
			.andExpect(jsonPath("$.errors[2].field").value("fiscalYear"))
			.andExpect(jsonPath("$.errors[3].field").value("text"))
			.andExpect(jsonPath("$.errors[3].message").value("is required and must not be blank"));
	}

	@Test
	void rejectsOutOfRangeFiscalPeriod() throws Exception {
		submit(request("KPITTECH", 1999, 5, "Transcript text"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors", hasSize(2)))
			.andExpect(jsonPath("$.errors[0].field").value("fiscalQuarter"))
			.andExpect(jsonPath("$.errors[0].message").value("must be between 1 and 4"))
			.andExpect(jsonPath("$.errors[1].field").value("fiscalYear"))
			.andExpect(jsonPath("$.errors[1].message").value("must be between 2000 and 2100"));
	}

	@Test
	void rejectsOversizedText() throws Exception {
		submit(request("KPITTECH", 2026, 1, "a".repeat(SubmitTranscriptRequest.MAX_TEXT_LENGTH + 1)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors", hasSize(1)))
			.andExpect(jsonPath("$.errors[0].field").value("text"))
			.andExpect(jsonPath("$.errors[0].message").value("must be at most 500000 characters"));
		assertThat(this.transcriptRepository.count()).isZero();
	}

	@Test
	void rejectsMalformedJson() throws Exception {
		submit("{\"companyTicker\": \"KPITTECH\", ")
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail")
				.value("Request body is missing or is not valid JSON with the expected field types."));
	}

	@Test
	void rejectsWrongFieldType() throws Exception {
		submit("""
				{"companyTicker": "KPITTECH", "fiscalYear": "last year", "fiscalQuarter": 1, "text": "Text"}
				""")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail")
				.value("Request body is missing or is not valid JSON with the expected field types."));
	}

	@Test
	void rejectsNonJsonContentType() throws Exception {
		this.mockMvc.perform(post("/api/transcripts").contentType(MediaType.TEXT_PLAIN).content("Transcript text"))
			.andExpect(status().isUnsupportedMediaType())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
	}

	@Test
	void rejectsUnknownCompanyTicker() throws Exception {
		submit(request("TCS", 2026, 1, "Transcript text"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value(
					"Unknown companyTicker 'TCS'. Supported tickers: COFORGE, KPITTECH, LTIM, NEWGEN, PERSISTENT."));
	}

	@Test
	void rejectsSecondTranscriptForSameCompanyAndQuarter() throws Exception {
		submit(request("NEWGEN", 2026, 3, "First transcript")).andExpect(status().isCreated());

		submit(request("NEWGEN", 2026, 3, "Second transcript"))
			.andExpect(status().isConflict())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail", containsString("NEWGEN FY2026 Q3 already exists")));
		assertThat(this.transcriptRepository.count()).isEqualTo(1);
	}

	private ResultActions submit(String json) throws Exception {
		return this.mockMvc
			.perform(post("/api/transcripts").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private static String request(String ticker, int fiscalYear, int fiscalQuarter, String text) {
		return """
				{"companyTicker": "%s", "fiscalYear": %d, "fiscalQuarter": %d, "text": "%s"}
				""".formatted(ticker, fiscalYear, fiscalQuarter, text);
	}

}
