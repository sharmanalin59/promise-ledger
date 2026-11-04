package com.promiseledger.extraction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.promiseledger.transcript.Transcript;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "extraction_runs")
public class ExtractionRun {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "id", length = 36, nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "transcript_id", nullable = false)
	private Transcript transcript;

	@Column(name = "provider", nullable = false, length = 40)
	private String provider;

	@Column(name = "model", nullable = false, length = 100)
	private String model;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "input_tokens")
	private Integer inputTokens;

	@Column(name = "output_tokens")
	private Integer outputTokens;

	@Column(name = "estimated_cost_usd", precision = 12, scale = 6)
	private BigDecimal estimatedCostUsd;

	@Column(name = "latency_ms", nullable = false)
	private long latencyMillis;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "failure_category", length = 40)
	private ClaudeFailureCategory failureCategory;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "status", nullable = false, length = 20)
	private ExtractionRunStatus status;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected ExtractionRun() {
	}

	public ExtractionRun(Transcript transcript, String provider, String model, int attemptCount, Integer inputTokens,
			Integer outputTokens, BigDecimal estimatedCostUsd, long latencyMillis, ClaudeFailureCategory failureCategory,
			ExtractionRunStatus status) {
		this.transcript = transcript;
		this.provider = provider;
		this.model = model;
		this.attemptCount = attemptCount;
		this.inputTokens = inputTokens;
		this.outputTokens = outputTokens;
		this.estimatedCostUsd = estimatedCostUsd;
		this.latencyMillis = latencyMillis;
		this.failureCategory = failureCategory;
		this.status = status;
	}

	public UUID getId() {
		return this.id;
	}

	public Transcript getTranscript() {
		return this.transcript;
	}

	public String getProvider() {
		return this.provider;
	}

	public String getModel() {
		return this.model;
	}

	public int getAttemptCount() {
		return this.attemptCount;
	}

	public Integer getInputTokens() {
		return this.inputTokens;
	}

	public Integer getOutputTokens() {
		return this.outputTokens;
	}

	public BigDecimal getEstimatedCostUsd() {
		return this.estimatedCostUsd;
	}

	public long getLatencyMillis() {
		return this.latencyMillis;
	}

	public ClaudeFailureCategory getFailureCategory() {
		return this.failureCategory;
	}

	public ExtractionRunStatus getStatus() {
		return this.status;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

}
