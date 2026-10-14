package com.promiseledger.transcript;

import java.util.UUID;

import com.promiseledger.company.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranscriptRepository extends JpaRepository<Transcript, UUID> {

	boolean existsByCompanyAndFiscalYearAndFiscalQuarter(Company company, int fiscalYear, int fiscalQuarter);

}
