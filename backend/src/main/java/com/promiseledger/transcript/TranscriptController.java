package com.promiseledger.transcript;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transcripts")
class TranscriptController {

	private final TranscriptService transcriptService;

	TranscriptController(TranscriptService transcriptService) {
		this.transcriptService = transcriptService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	TranscriptResponse submit(@Valid @RequestBody SubmitTranscriptRequest request) {
		return TranscriptResponse.from(this.transcriptService.submit(request));
	}

}
