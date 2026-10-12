package com.promiseledger.transcript;

import java.time.Instant;
import java.util.UUID;

import com.promiseledger.company.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "transcripts")
public class Transcript {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "id", length = 36, nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "company_id", nullable = false)
	private Company company;

	@Column(name = "fiscal_year", nullable = false)
	private int fiscalYear;

	@Column(name = "fiscal_quarter", nullable = false, columnDefinition = "tinyint")
	private int fiscalQuarter;

	@Column(name = "text", nullable = false, columnDefinition = "longtext")
	private String text;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Transcript() {
	}

	public Transcript(Company company, int fiscalYear, int fiscalQuarter, String text) {
		this.company = company;
		this.fiscalYear = fiscalYear;
		this.fiscalQuarter = fiscalQuarter;
		this.text = text;
	}

	public UUID getId() {
		return id;
	}

	public Company getCompany() {
		return company;
	}

	public int getFiscalYear() {
		return fiscalYear;
	}

	public int getFiscalQuarter() {
		return fiscalQuarter;
	}

	public String getText() {
		return text;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
