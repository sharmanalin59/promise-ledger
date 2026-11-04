package com.promiseledger.extraction;

import java.util.List;

import com.promiseledger.promise.Promise;

public record ExtractionOutcome(List<Promise> promises, ExtractionResponse.ExtractionUsage usage) {
}
