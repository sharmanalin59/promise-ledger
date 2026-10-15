package com.promiseledger.extraction;

import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ClaudeClient {

	private static final String API_VERSION = "2023-06-01";

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

	private final ClaudeProperties properties;

	private final RestClient restClient;

	@Autowired
	public ClaudeClient(ClaudeProperties properties) {
		this(properties, RestClient.builder().requestFactory(requestFactory(properties.timeout())));
	}

	ClaudeClient(ClaudeProperties properties, RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.restClient = restClientBuilder.baseUrl(properties.baseUrl())
			.defaultHeader("anthropic-version", API_VERSION)
			.build();
	}

	public boolean isConfigured() {
		return this.properties.hasApiKey();
	}

	/**
	 * Sends one user message and returns the concatenated text blocks of Claude's reply.
	 * @throws ClaudeTimeoutException if Claude does not respond within the configured timeout
	 * @throws ClaudeException for any other provider failure
	 */
	public String complete(String systemPrompt, String userMessage) {
		if (!isConfigured()) {
			throw new ClaudeException("Claude API key is not configured.");
		}
		MessagesRequest request = new MessagesRequest(this.properties.model(), this.properties.maxTokens(),
				systemPrompt, List.of(new Message("user", userMessage)));
		MessagesResponse response;
		try {
			response = this.restClient.post()
				.uri("/v1/messages")
				.header("x-api-key", this.properties.apiKey())
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(MessagesResponse.class);
		}
		catch (RestClientResponseException ex) {
			throw new ClaudeException("Claude API returned HTTP " + ex.getStatusCode().value() + ".", ex);
		}
		catch (ResourceAccessException ex) {
			if (isTimeout(ex)) {
				throw new ClaudeTimeoutException(
						"Claude API did not respond within " + this.properties.timeout().toSeconds() + " seconds.", ex);
			}
			throw new ClaudeException("Could not reach the Claude API.", ex);
		}
		catch (RestClientException ex) {
			throw new ClaudeException("Claude API returned an unreadable response.", ex);
		}
		if (response == null || response.content() == null) {
			throw new ClaudeException("Claude API returned no content.");
		}
		if ("max_tokens".equals(response.stopReason())) {
			throw new ClaudeException("Claude's reply was cut off at the max-tokens limit.");
		}
		return response.content()
			.stream()
			.filter((block) -> "text".equals(block.type()))
			.map(ContentBlock::text)
			.filter(Objects::nonNull)
			.collect(Collectors.joining());
	}

	private static boolean isTimeout(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
				return true;
			}
		}
		return false;
	}

	private static JdkClientHttpRequestFactory requestFactory(Duration timeout) {
		HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(timeout);
		return requestFactory;
	}

	record MessagesRequest(String model, @JsonProperty("max_tokens") int maxTokens, String system,
			List<Message> messages) {
	}

	record Message(String role, String content) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record MessagesResponse(List<ContentBlock> content, @JsonProperty("stop_reason") String stopReason) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	record ContentBlock(String type, String text) {
	}

}
