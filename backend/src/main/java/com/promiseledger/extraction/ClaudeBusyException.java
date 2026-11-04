package com.promiseledger.extraction;

public class ClaudeBusyException extends ClaudeException {

	public ClaudeBusyException() {
		super("Claude API concurrency limit was reached.", null, ClaudeFailureCategory.BUSY, 0, null, null, null,
				null);
	}

}
