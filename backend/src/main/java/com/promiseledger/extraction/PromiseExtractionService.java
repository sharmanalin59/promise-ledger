package com.promiseledger.extraction;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.promiseledger.extraction.ExtractionOutput.ExtractedPromise;
import com.promiseledger.promise.Promise;
import com.promiseledger.promise.PromiseImportance;
import com.promiseledger.promise.PromiseRepository;
import com.promiseledger.transcript.Transcript;
import com.promiseledger.transcript.TranscriptRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.ErrorResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class PromiseExtractionService {

	private static final Logger logger = LoggerFactory.getLogger(PromiseExtractionService.class);

	private final ClaudeClient claudeClient;

	private final TranscriptRepository transcriptRepository;

	private final PromiseRepository promiseRepository;

	private final TransactionTemplate transactionTemplate;

	private final JsonMapper jsonMapper;

	private final Validator validator;

	private final String systemPrompt;

	public PromiseExtractionService(ClaudeClient claudeClient, TranscriptRepository transcriptRepository,
			PromiseRepository promiseRepository, TransactionTemplate transactionTemplate, JsonMapper jsonMapper,
			Validator validator) {
		this.claudeClient = claudeClient;
		this.transcriptRepository = transcriptRepository;
		this.promiseRepository = promiseRepository;
		this.transactionTemplate = transactionTemplate;
		this.jsonMapper = jsonMapper;
		this.validator = validator;
		this.systemPrompt = loadPrompt();
	}

	public List<Promise> extract(UUID transcriptId) {
		Transcript transcript = this.transcriptRepository.findById(transcriptId)
			.orElseThrow(() -> problem(HttpStatus.NOT_FOUND, "Transcript " + transcriptId + " was not found.", null));
		requireNoExistingPromises(transcriptId);
		if (!this.claudeClient.isConfigured()) {
			throw problem(HttpStatus.SERVICE_UNAVAILABLE,
					"Promise extraction is unavailable: the Claude API key is not configured.", null);
		}

		logger.info("Extraction started for transcript {}", transcriptId);
		String output;
		try {
			output = this.claudeClient.complete(this.systemPrompt,
					"<transcript>\n" + transcript.getText() + "\n</transcript>");
		}
		catch (ClaudeTimeoutException ex) {
			logger.warn("Extraction failed for transcript {}: {}", transcriptId, ex.getMessage());
			throw problem(HttpStatus.GATEWAY_TIMEOUT, ex.getMessage() + " No promises were saved.", ex);
		}
		catch (ClaudeException ex) {
			logger.warn("Extraction failed for transcript {}: {}", transcriptId, ex.getMessage());
			throw problem(HttpStatus.BAD_GATEWAY, ex.getMessage() + " No promises were saved.", ex);
		}

		List<ExtractedPromise> extracted = parseAndValidate(transcriptId, output, transcript.getText());
		List<Promise> saved;
		try {
			saved = this.transactionTemplate.execute((status) -> {
				requireNoExistingPromises(transcriptId);
				return this.promiseRepository
					.saveAllAndFlush(extracted.stream().map((item) -> toPromise(transcript, item)).toList());
			});
		}
		catch (DataAccessException ex) {
			logger.error("Extraction failed for transcript {}: promises could not be saved", transcriptId, ex);
			throw problem(HttpStatus.INTERNAL_SERVER_ERROR,
					"Extracted promises could not be saved. No promises were saved.", ex);
		}
		logger.info("Extraction completed for transcript {}: {} promises saved", transcriptId, saved.size());
		return saved;
	}

	private List<ExtractedPromise> parseAndValidate(UUID transcriptId, String output, String transcriptText) {
		String json = stripMarkdownFence(output);
		if (json.isEmpty()) {
			throw invalidOutput(transcriptId, "Claude returned an empty response.", List.of());
		}
		ExtractionOutput parsed;
		try {
			parsed = this.jsonMapper.readValue(json, ExtractionOutput.class);
		}
		catch (JacksonException ex) {
			throw invalidOutput(transcriptId, "Claude returned malformed JSON.", List.of());
		}
		if (parsed == null) {
			throw invalidOutput(transcriptId, "Claude returned malformed JSON.", List.of());
		}

		List<String> errors = new ArrayList<>(this.validator.validate(parsed)
			.stream()
			.sorted(Comparator.comparing((ConstraintViolation<ExtractionOutput> violation) -> violation
				.getPropertyPath()
				.toString()))
			.map((violation) -> violation.getPropertyPath() + ": " + violation.getMessage())
			.toList());
		if (errors.isEmpty()) {
			String normalizedTranscript = normalize(transcriptText);
			for (int i = 0; i < parsed.promises().size(); i++) {
				if (!normalizedTranscript.contains(normalize(parsed.promises().get(i).sourceQuote()))) {
					errors.add("promises[" + i + "].sourceQuote: was not found in the transcript");
				}
			}
		}
		if (!errors.isEmpty()) {
			throw invalidOutput(transcriptId, "Claude returned promises that failed validation.", errors);
		}
		return parsed.promises();
	}

	private void requireNoExistingPromises(UUID transcriptId) {
		if (this.promiseRepository.existsByTranscriptId(transcriptId)) {
			throw problem(HttpStatus.CONFLICT,
					"Promises have already been extracted for transcript " + transcriptId + ".", null);
		}
	}

	private static Promise toPromise(Transcript transcript, ExtractedPromise item) {
		Promise promise = new Promise(transcript, item.statement().strip(),
				PromiseImportance.valueOf(item.importance()));
		promise.setSourceQuote(item.sourceQuote().strip());
		promise.setCategory(blankToNull(item.category()));
		promise.setTimeframe(blankToNull(item.timeframe()));
		return promise;
	}

	private static ErrorResponseException invalidOutput(UUID transcriptId, String detail, List<String> errors) {
		logger.warn("Extraction failed for transcript {}: {} {}", transcriptId, detail, errors);
		ErrorResponseException ex = problem(HttpStatus.BAD_GATEWAY, detail + " No promises were saved.", null);
		if (!errors.isEmpty()) {
			ex.getBody().setProperty("errors", errors);
		}
		return ex;
	}

	private static ErrorResponseException problem(HttpStatus status, String detail, Throwable cause) {
		return new ErrorResponseException(status, ProblemDetail.forStatusAndDetail(status, detail), cause);
	}

	private static String stripMarkdownFence(String output) {
		String text = (output != null) ? output.strip() : "";
		if (text.startsWith("```")) {
			int firstLineEnd = text.indexOf('\n');
			text = (firstLineEnd >= 0) ? text.substring(firstLineEnd + 1) : "";
			if (text.endsWith("```")) {
				text = text.substring(0, text.length() - 3);
			}
		}
		return text.strip();
	}

	static String normalize(String text) {
		return text.replace('\u2018', '\'')
			.replace('\u2019', '\'')
			.replace('\u201C', '"')
			.replace('\u201D', '"')
			.replace('\u2013', '-')
			.replace('\u2014', '-')
			.replaceAll("\\s+", " ")
			.strip()
			.toLowerCase(Locale.ROOT);
	}

	private static String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.strip();
	}

	private static String loadPrompt() {
		try {
			return new ClassPathResource("prompts/promise-extraction.txt").getContentAsString(StandardCharsets.UTF_8);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Could not load the promise extraction prompt", ex);
		}
	}

}
