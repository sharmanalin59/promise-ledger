package com.promiseledger.transcript;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitTranscriptRequest(
		@NotBlank(message = "is required") String companyTicker,
		@NotNull(message = "is required") @Min(value = 2000, message = "must be between 2000 and 2100")
		@Max(value = 2100, message = "must be between 2000 and 2100") Integer fiscalYear,
		@NotNull(message = "is required") @Min(value = 1, message = "must be between 1 and 4")
		@Max(value = 4, message = "must be between 1 and 4") Integer fiscalQuarter,
		@NotBlank(message = "is required and must not be blank")
		@Size(max = SubmitTranscriptRequest.MAX_TEXT_LENGTH,
				message = "must be at most " + SubmitTranscriptRequest.MAX_TEXT_LENGTH + " characters") String text) {

	public static final int MAX_TEXT_LENGTH = 500_000;

}
