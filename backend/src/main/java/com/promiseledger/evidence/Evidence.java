package com.promiseledger.evidence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.promiseledger.promise.Promise;
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
@Table(name = "evidence")
public class Evidence {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "id", length = 36, nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "promise_id", nullable = false)
	private Promise promise;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "source_type", nullable = false, length = 40)
	private EvidenceSourceType sourceType;

	@Column(name = "text", nullable = false, columnDefinition = "text")
	private String text;

	@Column(name = "confidence", precision = 5, scale = 4)
	private BigDecimal confidence;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Evidence() {
	}

	public Evidence(Promise promise, EvidenceSourceType sourceType, String text, BigDecimal confidence) {
		this.promise = promise;
		this.sourceType = sourceType;
		this.text = text;
		this.confidence = confidence;
	}

	public UUID getId() {
		return id;
	}

	public Promise getPromise() {
		return promise;
	}

	public EvidenceSourceType getSourceType() {
		return sourceType;
	}

	public String getText() {
		return text;
	}

	public BigDecimal getConfidence() {
		return confidence;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
