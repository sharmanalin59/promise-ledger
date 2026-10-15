package com.promiseledger.extraction;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ClaudeClientTests {

	private static final ClaudeProperties PROPERTIES = new ClaudeProperties("test-key", "claude-test-model",
			"https://claude.test", Duration.ofSeconds(30), 1000);

	private final RestClient.Builder builder = RestClient.builder();

	private final MockRestServiceServer server = MockRestServiceServer.bindTo(this.builder).build();

	private final ClaudeClient client = new ClaudeClient(PROPERTIES, this.builder);

	@Test
	void sendsMessagesRequestAndReturnsOnlyTextBlocks() {
		this.server.expect(requestTo("https://claude.test/v1/messages"))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header("x-api-key", "test-key"))
			.andExpect(header("anthropic-version", "2023-06-01"))
			.andExpect(jsonPath("$.model").value("claude-test-model"))
			.andExpect(jsonPath("$.max_tokens").value(1000))
			.andExpect(jsonPath("$.system").value("system prompt"))
			.andExpect(jsonPath("$.messages[0].role").value("user"))
			.andExpect(jsonPath("$.messages[0].content").value("transcript"))
			.andRespond(withSuccess("""
					{"content": [{"type": "thinking", "thinking": "..."},
					             {"type": "text", "text": "{\\"promises\\": "},
					             {"type": "text", "text": "[]}"}],
					 "stop_reason": "end_turn", "usage": {"input_tokens": 10}}
					""", MediaType.APPLICATION_JSON));

		assertThat(this.client.complete("system prompt", "transcript")).isEqualTo("{\"promises\": []}");
		this.server.verify();
	}

	@ParameterizedTest
	@ValueSource(ints = { 400, 401, 429, 500, 529 })
	void reportsHttpErrorsAsProviderFailures(int status) {
		this.server.expect(requestTo("https://claude.test/v1/messages"))
			.andRespond(withRawStatus(status));

		assertThatThrownBy(() -> this.client.complete("system", "user")).isExactlyInstanceOf(ClaudeException.class)
			.hasMessage("Claude API returned HTTP " + status + ".");
	}

	@Test
	void reportsReadTimeoutAsTimeout() {
		this.server.expect(requestTo("https://claude.test/v1/messages"))
			.andRespond(withException(new SocketTimeoutException("Read timed out")));

		assertThatThrownBy(() -> this.client.complete("system", "user")).isInstanceOf(ClaudeTimeoutException.class)
			.hasMessage("Claude API did not respond within 30 seconds.");
	}

	@Test
	void reportsConnectionFailureAsProviderFailure() {
		this.server.expect(requestTo("https://claude.test/v1/messages"))
			.andRespond(withException(new ConnectException("Connection refused")));

		assertThatThrownBy(() -> this.client.complete("system", "user")).isExactlyInstanceOf(ClaudeException.class)
			.hasMessage("Could not reach the Claude API.");
	}

	@Test
	void reportsTruncatedReply() {
		this.server.expect(requestTo("https://claude.test/v1/messages"))
			.andRespond(withSuccess("""
					{"content": [{"type": "text", "text": "{\\"promises\\": ["}], "stop_reason": "max_tokens"}
					""", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> this.client.complete("system", "user")).isExactlyInstanceOf(ClaudeException.class)
			.hasMessage("Claude's reply was cut off at the max-tokens limit.");
	}

	@Test
	void reportsUnreadableResponse() {
		this.server.expect(requestTo("https://claude.test/v1/messages"))
			.andRespond(withSuccess("<html>gateway error</html>", MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> this.client.complete("system", "user")).isExactlyInstanceOf(ClaudeException.class)
			.hasMessage("Claude API returned an unreadable response.");
	}

	@Test
	void refusesToCallWithoutApiKey() {
		ClaudeClient unconfigured = new ClaudeClient(
				new ClaudeProperties(" ", "model", "https://claude.test", Duration.ofSeconds(30), 1000), this.builder);

		assertThat(unconfigured.isConfigured()).isFalse();
		assertThatThrownBy(() -> unconfigured.complete("system", "user")).isExactlyInstanceOf(ClaudeException.class)
			.hasMessage("Claude API key is not configured.");
	}

	@Test
	void propertiesNeverExposeApiKeyInToString() {
		assertThat(PROPERTIES.toString()).doesNotContain("test-key").contains("apiKey=****");
	}

}
