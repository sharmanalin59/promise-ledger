package com.promiseledger.company;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class CompanyController {

	private final CompanyQueryService companyQueryService;

	CompanyController(CompanyQueryService companyQueryService) {
		this.companyQueryService = companyQueryService;
	}

	@GetMapping("/api/companies")
	List<CompanyResponse> list() {
		return this.companyQueryService.listCompanies();
	}

	@GetMapping("/api/companies/{companyId}")
	CompanyLedgerResponse company(@PathVariable UUID companyId) {
		return this.companyQueryService.getLedger(companyId);
	}

}
