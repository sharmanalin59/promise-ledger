package com.promiseledger.promise;

import java.util.UUID;

import com.promiseledger.evidence.EvidenceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;

@Service
public class PromiseQueryService {

	private final PromiseRepository promiseRepository;

	private final EvidenceRepository evidenceRepository;

	public PromiseQueryService(PromiseRepository promiseRepository, EvidenceRepository evidenceRepository) {
		this.promiseRepository = promiseRepository;
		this.evidenceRepository = evidenceRepository;
	}

	@Transactional(readOnly = true)
	public PromiseDetailResponse getPromise(UUID promiseId) {
		Promise promise = this.promiseRepository.findById(promiseId)
			.orElseThrow(() -> new ErrorResponseException(HttpStatus.NOT_FOUND,
					ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Promise " + promiseId + " was not found."),
					null));
		// Touch associations while the session is open.
		promise.getTranscript().getCompany().getName();
		return PromiseDetailResponse.from(promise, this.evidenceRepository.findByPromiseId(promiseId));
	}

}
