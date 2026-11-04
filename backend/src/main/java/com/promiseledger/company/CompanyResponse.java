package com.promiseledger.company;

import java.util.UUID;

public record CompanyResponse(UUID id, String name, String ticker) {

	static CompanyResponse from(Company company) {
		return new CompanyResponse(company.getId(), company.getName(), company.getTicker());
	}

}
