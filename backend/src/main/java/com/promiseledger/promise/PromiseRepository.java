package com.promiseledger.promise;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PromiseRepository extends JpaRepository<Promise, UUID> {

	List<Promise> findByTranscriptCompanyId(UUID companyId);

	boolean existsByTranscriptId(UUID transcriptId);

}
