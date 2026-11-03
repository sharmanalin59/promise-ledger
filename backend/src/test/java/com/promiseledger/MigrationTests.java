package com.promiseledger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.testcontainers.mysql.MySQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigrationTests {

	private static final String DATABASE = "migration_check";

	@Autowired
	private MySQLContainer mysql;

	@Test
	void addingSourceQuotePreservesExistingPromises() throws SQLException {
		try (Connection root = DriverManager.getConnection(this.mysql.getJdbcUrl(), "root", this.mysql.getPassword())) {
			root.createStatement().execute("DROP DATABASE IF EXISTS " + DATABASE);
			root.createStatement().execute("CREATE DATABASE " + DATABASE);
		}
		String url = this.mysql.getJdbcUrl().replace("/" + this.mysql.getDatabaseName(), "/" + DATABASE);
		SingleConnectionDataSource dataSource = new SingleConnectionDataSource(url, "root", this.mysql.getPassword(),
				true);
		try {
			Flyway.configure().dataSource(dataSource).target("2").load().migrate();
			JdbcTemplate jdbc = new JdbcTemplate(dataSource);
			jdbc.update("""
					INSERT INTO transcripts (id, company_id, fiscal_year, fiscal_quarter, text, created_at, updated_at)
					SELECT 't-1', id, 2025, 4, 'Older transcript', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
					FROM companies WHERE ticker = 'KPITTECH'
					""");
			jdbc.update("""
					INSERT INTO promises (id, transcript_id, statement, importance, status, created_at, updated_at)
					VALUES ('p-1', 't-1', 'Margins will improve next year.', 'HIGH', 'DELIVERED',
					        UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
					""");
			jdbc.update("""
					INSERT INTO evidence (id, promise_id, source_type, text, created_at, updated_at)
					VALUES ('e-1', 'p-1', 'FINANCIAL_STATEMENT', 'Q4 results show margin expansion.',
					        UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))
					""");

			Flyway.configure().dataSource(dataSource).load().migrate();

			Map<String, Object> promise = jdbc.queryForMap("SELECT * FROM promises WHERE id = 'p-1'");
			assertThat(promise).containsEntry("statement", "Margins will improve next year.")
				.containsEntry("importance", "HIGH")
				.containsEntry("status", "DELIVERED")
				.containsEntry("source_quote", null);
			assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM evidence WHERE promise_id = 'p-1'", Integer.class))
				.isEqualTo(1);
			assertThat(jdbc.queryForObject("""
					SELECT is_nullable FROM information_schema.columns
					WHERE table_schema = DATABASE() AND table_name = 'promises' AND column_name = 'source_quote'
					""", String.class)).isEqualTo("YES");
		}
		finally {
			dataSource.destroy();
		}
	}

}
