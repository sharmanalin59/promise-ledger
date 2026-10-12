package com.promiseledger.promise;

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
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "promises")
public class Promise {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "id", length = 36, nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "transcript_id", nullable = false)
	private Transcript transcript;

	@Column(name = "statement", nullable = false, columnDefinition = "text")
	private String statement;

	@Column(name = "category", length = 100)
	private String category;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "importance", nullable = false, length = 20)
	private PromiseImportance importance;

	@Column(name = "progress_percent", precision = 5, scale = 2)
	private BigDecimal progressPercent;

	@Column(name = "timeframe")
	private String timeframe;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "status", nullable = false, length = 20)
	private PromiseStatus status = PromiseStatus.OPEN;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Promise() {
	}

	public Promise(Transcript transcript, String statement, PromiseImportance importance) {
		this.transcript = transcript;
		this.statement = statement;
		this.importance = importance;
	}

	public UUID getId() {
		return id;
	}

	public Transcript getTranscript() {
		return transcript;
	}

	public String getStatement() {
		return statement;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public PromiseImportance getImportance() {
		return importance;
	}

	public BigDecimal getProgressPercent() {
		return progressPercent;
	}

	public void setProgressPercent(BigDecimal progressPercent) {
		this.progressPercent = progressPercent;
	}

	public String getTimeframe() {
		return timeframe;
	}

	public void setTimeframe(String timeframe) {
		this.timeframe = timeframe;
	}

	public PromiseStatus getStatus() {
		return status;
	}

	public void setStatus(PromiseStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
