package com.promiseledger.extraction;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("claude")
public record ClaudeProperties(String apiKey, String model, String baseUrl, Duration timeout, int maxTokens) {

	public boolean hasApiKey() {
		return this.apiKey != null && !this.apiKey.isBlank();
	}

	@Override
	public String toString() {
		return "ClaudeProperties[apiKey=" + (hasApiKey() ? "****" : "<not set>") + ", model=" + this.model
				+ ", baseUrl=" + this.baseUrl + ", timeout=" + this.timeout + ", maxTokens=" + this.maxTokens + "]";
	}

}
