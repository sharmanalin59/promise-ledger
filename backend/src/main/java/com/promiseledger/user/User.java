package com.promiseledger.user;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "id", length = 36, nullable = false, updatable = false)
	private UUID id;

	@Column(name = "username", nullable = false)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected User() {
	}

	public User(String username, String passwordHash) {
		this.username = username;
		this.passwordHash = passwordHash;
	}

	public UUID getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	@Override
	public String toString() {
		return "User[id=" + this.id + ", username=" + this.username + ", passwordHash=****]";
	}

}
