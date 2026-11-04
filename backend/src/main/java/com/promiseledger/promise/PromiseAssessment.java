package com.promiseledger.promise;

/**
 * UI-facing assessment label. OPEN is never shown as a miss — silence is not failure.
 */
public final class PromiseAssessment {

	private PromiseAssessment() {
	}

	public static String labelFor(PromiseStatus status) {
		return switch (status) {
			case OPEN -> "Unassessed";
			case DELIVERED -> "Delivered";
			case PARTIAL -> "Partial";
			case MISSED -> "Missed";
			case EXPIRED -> "Expired";
		};
	}

}
