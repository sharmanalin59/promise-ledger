package com.promiseledger;

import java.util.List;

import com.promiseledger.company.CompanyRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PromiseLedgerApplicationTests {

	@Autowired
	private Flyway flyway;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private CompanyRepository companyRepository;

	@Test
	void flywayAppliesAllMigrations() {
		MigrationInfo[] applied = this.flyway.info().applied();

		assertThat(applied).extracting((info) -> info.getVersion().getVersion()).containsExactly("1", "2", "3");
		assertThat(applied).allSatisfy((info) -> assertThat(info.getState().isFailed()).isFalse());
	}

	@Test
	void schemaContainsExactlyTheFourMvpTables() {
		List<String> tables = this.jdbcTemplate.queryForList("""
				SELECT table_name FROM information_schema.tables
				WHERE table_schema = DATABASE() AND table_name <> 'flyway_schema_history'
				""", String.class);

		assertThat(tables).containsExactlyInAnyOrder("companies", "transcripts", "promises", "evidence");
	}

	@Test
	void seedsTheFiveMvpCompanies() {
		assertThat(this.companyRepository.findAll()).extracting("ticker")
			.containsExactlyInAnyOrder("KPITTECH", "COFORGE", "NEWGEN", "PERSISTENT", "LTIM");
	}

}
