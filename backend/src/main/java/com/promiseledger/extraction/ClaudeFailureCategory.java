package com.promiseledger.extraction;

/**
 * Why a Claude call ended without a usable reply. Retryable categories are attempted again, up to the configured
 * bound. Categories that count toward the circuit are provider failures rather than bad requests of our own.
 */
public enum ClaudeFailureCategory {

	TIMEOUT(true, true),
	RATE_LIMITED(true, true),
	TRANSIENT(true, true),
	CLIENT_ERROR(false, false),
	CIRCUIT_OPEN(false, false),
	INPUT_TOO_LARGE(false, false),
	TRUNCATED(false, false),
	UNREADABLE(false, false),
	NOT_CONFIGURED(false, false),
	BUSY(false, false),
	PERSISTENCE(false, false),
	INVALID_OUTPUT(false, false);

	private final boolean retryable;

	private final boolean countsTowardCircuit;

	ClaudeFailureCategory(boolean retryable, boolean countsTowardCircuit) {
		this.retryable = retryable;
		this.countsTowardCircuit = countsTowardCircuit;
	}

	public boolean retryable() {
		return this.retryable;
	}

	public boolean countsTowardCircuit() {
		return this.countsTowardCircuit;
	}

}
