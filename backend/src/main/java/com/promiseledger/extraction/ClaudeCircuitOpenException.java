package com.promiseledger.extraction;

public class ClaudeCircuitOpenException extends ClaudeException {

	public ClaudeCircuitOpenException() {
		super("Claude API is temporarily unavailable after repeated failures.", null,
				ClaudeFailureCategory.CIRCUIT_OPEN, 0, null, null, null, null);
	}

}
