package com.promiseledger.extraction;

public class ClaudeInputTooLargeException extends ClaudeException {

	public ClaudeInputTooLargeException(int estimatedTokens, int limit) {
		super("Transcript is too large to send to Claude (estimated " + estimatedTokens + " tokens, limit " + limit
				+ ").", null, ClaudeFailureCategory.INPUT_TOO_LARGE, 0, null, null, null, null);
	}

}
