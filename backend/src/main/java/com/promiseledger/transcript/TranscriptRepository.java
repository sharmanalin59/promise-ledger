package com.promiseledger.transcript;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TranscriptRepository extends JpaRepository<Transcript, UUID> {

}
