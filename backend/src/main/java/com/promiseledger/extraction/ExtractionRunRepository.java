package com.promiseledger.extraction;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtractionRunRepository extends JpaRepository<ExtractionRun, UUID> {

	List<ExtractionRun> findByTranscriptIdOrderByCreatedAtDesc(UUID transcriptId);

}
