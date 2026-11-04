package com.promiseledger.extraction;

public class ClaudeTimeoutException extends ClaudeException {

	public ClaudeTimeoutException(String message, Throwable cause) {
		this(message, cause, 1);
	}

	ClaudeTimeoutException(String message, Throwable cause, int attempts) {
		super(message, cause, ClaudeFailureCategory.TIMEOUT, attempts, null, null, null, null);
	}

	@Override
	public ClaudeTimeoutException withAttempts(int attempts) {
		return new ClaudeTimeoutException(getMessage(), getCause(), attempts);
	}

}
