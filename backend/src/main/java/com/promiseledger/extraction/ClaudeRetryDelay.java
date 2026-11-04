package com.promiseledger.extraction;

import java.time.Duration;
import java.util.Random;

/**
 * Wait before another attempt. A {@code Retry-After} value is used as given, capped at the configured maximum.
 * Otherwise the wait grows exponentially and is scaled by a jitter factor in {@code [0.5, 1]}.
 */
final class ClaudeRetryDelay {

	private ClaudeRetryDelay() {
	}

	static Duration next(int failedAttempt, Duration initial, Duration max, Random random, Duration retryAfter) {
		Duration chosen = (retryAfter != null) ? nonNegative(retryAfter) : exponential(failedAttempt, initial, random);
		return (chosen.compareTo(max) > 0) ? max : chosen;
	}

	private static Duration nonNegative(Duration duration) {
		return duration.isNegative() ? Duration.ZERO : duration;
	}

	private static Duration exponential(int failedAttempt, Duration initial, Random random) {
		int shift = Math.min(Math.max(failedAttempt, 1) - 1, 20);
		long base = initial.toMillis() << shift;
		double factor = 0.5d + (random.nextDouble() * 0.5d);
		return Duration.ofMillis(Math.round(base * factor));
	}

}
