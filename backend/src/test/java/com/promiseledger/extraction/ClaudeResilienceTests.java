package com.promiseledger.extraction;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ClaudeResilienceTests {

	private static final String URL = "https://claude.test/v1/messages";

	private static final String OK = """
			{"content": [{"type": "text", "text": "{\\"promises\\": []}"}],
			 "stop_reason": "end_turn",
			 "usage": {"input_tokens": 1000, "output_tokens": 500}}
			""";

	private final List<Duration> sleeps = new ArrayList<>();

	private final Random jitterFloor = new Random() {
		@Override
		public double nextDouble() {
			return 0.0;
		}
	};

	@Test
	void retriesTransientFailureThenReturnsUsageAndCost() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(URL)).andRespond(withRawStatus(500));
		server.expect(requestTo(URL)).andRespond(withSuccess(OK, MediaType.APPLICATION_JSON));
		ClaudeClient client = client(builder, properties(3, 100, 4, Duration.ofSeconds(1), 100_000), Clock.systemUTC());

		ClaudeCompletion completion = client.completeDetailed("system", "user");

		assertThat(completion.text()).isEqualTo("{\"promises\": []}");
		assertThat(completion.attempts()).isEqualTo(2);
		assertThat(completion.inputTokens()).isEqualTo(1000);
		assertThat(completion.outputTokens()).isEqualTo(500);
		assertThat(completion.estimatedCostUsd()).isEqualByComparingTo(new BigDecimal("0.010500"));
		assertThat(this.sleeps).containsExactly(Duration.ofMillis(100));
		server.verify();
	}

	@Test
	void waitsForRetryAfterOnTooManyRequests() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(URL)).andRespond(withRawStatus(429).header("Retry-After", "2"));
		server.expect(requestTo(URL)).andRespond(withSuccess(OK, MediaType.APPLICATION_JSON));
		ClaudeClient client = client(builder, properties(3, 100, 4, Duration.ofSeconds(1), 100_000), Clock.systemUTC());

		assertThat(client.complete("system", "user")).isEqualTo("{\"promises\": []}");
		assertThat(this.sleeps).containsExactly(Duration.ofSeconds(2));
		server.verify();
	}

	@Test
	void doesNotRetryClientErrors() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(URL)).andRespond(withRawStatus(400));
		ClaudeClient client = client(builder, properties(3, 100, 4, Duration.ofSeconds(1), 100_000), Clock.systemUTC());

		Throwable thrown = catchThrowable(() -> client.complete("system", "user"));

		assertThat(thrown).isExactlyInstanceOf(ClaudeException.class).hasMessage("Claude API returned HTTP 400.");
		assertThat(((ClaudeException) thrown).attempts()).isEqualTo(1);
		assertThat(this.sleeps).isEmpty();
		server.verify();
	}

	@Test
	void boundsTimeoutRetries() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));
		server.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));
		ClaudeClient client = client(builder, properties(2, 100, 4, Duration.ofSeconds(1), 100_000), Clock.systemUTC());

		Throwable thrown = catchThrowable(() -> client.complete("system", "user"));

		assertThat(thrown).isInstanceOf(ClaudeTimeoutException.class);
		assertThat(((ClaudeException) thrown).attempts()).isEqualTo(2);
		assertThat(this.sleeps).hasSize(1);
		server.verify();
	}

	@Test
	void openCircuitSkipsTheProviderUntilTheWindowPasses() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(URL)).andRespond(withRawStatus(503));
		server.expect(requestTo(URL)).andRespond(withRawStatus(503));
		server.expect(requestTo(URL)).andRespond(withSuccess(OK, MediaType.APPLICATION_JSON));
		MutableClock clock = new MutableClock(Instant.parse("2026-11-03T00:00:00Z"));
		ClaudeClient client = client(builder, properties(1, 2, 4, Duration.ofSeconds(1), 100_000), clock);

		assertThatThrownBy(() -> client.complete("s", "u")).isInstanceOf(ClaudeException.class);
		assertThatThrownBy(() -> client.complete("s", "u")).isInstanceOf(ClaudeException.class);
		assertThatThrownBy(() -> client.complete("s", "u")).isExactlyInstanceOf(ClaudeCircuitOpenException.class);

		clock.advance(Duration.ofMinutes(1));
		assertThat(client.complete("s", "u")).isEqualTo("{\"promises\": []}");
		server.verify();
	}

	@Test
	void rejectsOversizedInputBeforeHttp() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		ClaudeClient client = client(builder, properties(3, 100, 4, Duration.ofSeconds(1), 1), Clock.systemUTC());

		assertThatThrownBy(() -> client.complete("system prompt", "transcript"))
			.isExactlyInstanceOf(ClaudeInputTooLargeException.class)
			.hasMessageContaining("limit 1");
		server.verify();
	}

	@Test
	void boundsConcurrentProviderCalls() throws Exception {
		AtomicInteger inFlight = new AtomicInteger();
		AtomicInteger maxInFlight = new AtomicInteger();
		RestClient.Builder builder = RestClient.builder().requestInterceptor((request, body, execution) -> {
			int now = inFlight.incrementAndGet();
			maxInFlight.accumulateAndGet(now, Math::max);
			try {
				Thread.sleep(200);
				return execution.execute(request, body);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new java.io.IOException("interrupted", ex);
			}
			finally {
				inFlight.decrementAndGet();
			}
		});
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(URL)).andRespond(withSuccess(OK, MediaType.APPLICATION_JSON));
		server.expect(requestTo(URL)).andRespond(withSuccess(OK, MediaType.APPLICATION_JSON));
		ClaudeClient client = client(builder, properties(1, 100, 1, Duration.ofSeconds(5), 100_000), Clock.systemUTC());
		ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			Future<String> first = pool.submit(() -> client.complete("s", "u"));
			Future<String> second = pool.submit(() -> client.complete("s", "u"));
			assertThat(first.get()).isEqualTo("{\"promises\": []}");
			assertThat(second.get()).isEqualTo("{\"promises\": []}");
		}
		finally {
			pool.shutdownNow();
		}
		assertThat(maxInFlight.get()).isEqualTo(1);
		server.verify();
	}

	@Test
	void concurrencyLimiterRejectsWhenTheWaitExpires() {
		ClaudeConcurrencyLimiter limiter = new ClaudeConcurrencyLimiter(1, Duration.ofMillis(20));
		limiter.acquire();
		assertThatThrownBy(limiter::acquire).isExactlyInstanceOf(ClaudeBusyException.class);
		limiter.release();
		limiter.acquire();
		limiter.release();
	}

	private ClaudeClient client(RestClient.Builder builder, ClaudeProperties properties, Clock clock) {
		return new ClaudeClient(properties, builder, clock, this.sleeps::add, this.jitterFloor);
	}

	private static ClaudeProperties properties(int maxAttempts, int failureThreshold, int maxConcurrent, Duration wait,
			int maxInputTokens) {
		return new ClaudeProperties("test-key", "claude-test-model", "https://claude.test", Duration.ofSeconds(10),
				Duration.ofSeconds(30), 1000, maxAttempts, Duration.ofMillis(200), Duration.ofSeconds(10),
				failureThreshold, Duration.ofMinutes(1), maxConcurrent, wait, maxInputTokens, new BigDecimal("3"),
				new BigDecimal("15"));
	}

	private static final class MutableClock extends Clock {

		private Instant instant;

		private MutableClock(Instant instant) {
			this.instant = instant;
		}

		private void advance(Duration duration) {
			this.instant = this.instant.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return this.instant;
		}

	}

}
