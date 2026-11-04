package com.promiseledger.extraction;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

final class ClaudeConcurrencyLimiter {

	private final Semaphore permits;

	private final Duration wait;

	ClaudeConcurrencyLimiter(int maxConcurrentCalls, Duration wait) {
		this.permits = new Semaphore(maxConcurrentCalls, true);
		this.wait = wait;
	}

	void acquire() {
		try {
			if (!this.permits.tryAcquire(this.wait.toMillis(), TimeUnit.MILLISECONDS)) {
				throw new ClaudeBusyException();
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new ClaudeException("Claude API call was interrupted.", ex);
		}
	}

	void release() {
		this.permits.release();
	}

}
