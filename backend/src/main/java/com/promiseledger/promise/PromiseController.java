package com.promiseledger.promise;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PromiseController {

	private final PromiseQueryService promiseQueryService;

	PromiseController(PromiseQueryService promiseQueryService) {
		this.promiseQueryService = promiseQueryService;
	}

	@GetMapping("/api/promises/{promiseId}")
	PromiseDetailResponse get(@PathVariable UUID promiseId) {
		return this.promiseQueryService.getPromise(promiseId);
	}

}
