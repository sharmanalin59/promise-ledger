package com.promiseledger.extraction;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Opens after a run of provider failures and rejects calls until the open interval has passed. The next call is a
 * single half-open trial.
 */
final class ClaudeCircuitBreaker {

	private final int failureThreshold;

	private final Duration openDuration;

	private final Clock clock;

	private int consecutiveFailures;

	private Instant openUntil;

	private boolean halfOpenTrialInFlight;

	ClaudeCircuitBreaker(int failureThreshold, Duration openDuration, Clock clock) {
		this.failureThreshold = failureThreshold;
		this.openDuration = openDuration;
		this.clock = clock;
	}

	synchronized void beforeCall() {
		Instant now = this.clock.instant();
		if (this.openUntil != null && now.isBefore(this.openUntil)) {
			throw new ClaudeCircuitOpenException();
		}
		if (this.openUntil != null) {
			if (this.halfOpenTrialInFlight) {
				throw new ClaudeCircuitOpenException();
			}
			this.halfOpenTrialInFlight = true;
		}
	}

	synchronized void recordSuccess() {
		this.consecutiveFailures = 0;
		this.openUntil = null;
		this.halfOpenTrialInFlight = false;
	}

	synchronized void recordFailure() {
		boolean trialFailed = this.halfOpenTrialInFlight;
		this.halfOpenTrialInFlight = false;
		if (trialFailed) {
			this.openUntil = this.clock.instant().plus(this.openDuration);
			this.consecutiveFailures = 0;
			return;
		}
		this.consecutiveFailures++;
		if (this.consecutiveFailures >= this.failureThreshold) {
			this.openUntil = this.clock.instant().plus(this.openDuration);
			this.consecutiveFailures = 0;
		}
	}

	synchronized void recordProviderResponded() {
		if (this.halfOpenTrialInFlight) {
			recordSuccess();
		}
	}

	synchronized void abandonTrial() {
		this.halfOpenTrialInFlight = false;
	}

}
