package com.promiseledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PromiseLedgerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PromiseLedgerApplication.class, args);
	}

}
