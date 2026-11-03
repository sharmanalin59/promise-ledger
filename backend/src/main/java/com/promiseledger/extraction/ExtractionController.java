package com.promiseledger.extraction;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ExtractionController {

	private final PromiseExtractionService extractionService;

	ExtractionController(PromiseExtractionService extractionService) {
		this.extractionService = extractionService;
	}

	@PostMapping("/api/transcripts/{transcriptId}/extract")
	ExtractionResponse extract(@PathVariable UUID transcriptId) {
		return ExtractionResponse.from(transcriptId, this.extractionService.extract(transcriptId));
	}

}
