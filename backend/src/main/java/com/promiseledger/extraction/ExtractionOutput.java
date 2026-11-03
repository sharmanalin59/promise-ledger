package com.promiseledger.extraction;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
record ExtractionOutput(@NotNull(message = "is required") @Size(max = ExtractionOutput.MAX_PROMISES,
		message = "must contain at most " + ExtractionOutput.MAX_PROMISES + " items")
List<@NotNull(message = "must not be null") @Valid ExtractedPromise> promises) {

	static final int MAX_PROMISES = 100;

	@JsonIgnoreProperties(ignoreUnknown = true)
	record ExtractedPromise(
			@NotBlank(message = "is required") @Size(max = 2000,
					message = "must be at most 2000 characters") String statement,
			@NotBlank(message = "is required") @Size(max = 2000,
					message = "must be at most 2000 characters") String sourceQuote,
			@Size(max = 100, message = "must be at most 100 characters") String category,
			@NotNull(message = "is required") @Pattern(regexp = "HIGH|MEDIUM|LOW",
					message = "must be HIGH, MEDIUM, or LOW") String importance,
			@Size(max = 255, message = "must be at most 255 characters") String timeframe) {
	}

}
