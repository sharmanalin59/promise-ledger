package com.promiseledger;

import com.promiseledger.user.User;
import com.promiseledger.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthApiTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void provisionsDemoUserWithHashedPassword() {
		User demo = this.userRepository.findByUsernameIgnoreCase("demo").orElseThrow();
		assertThat(demo.getPasswordHash()).startsWith("$2");
		assertThat(demo.getPasswordHash()).doesNotContainIgnoringCase("demo");
		assertThat(this.passwordEncoder.matches("demo", demo.getPasswordHash())).isTrue();
		assertThat(demo.toString()).doesNotContain(demo.getPasswordHash()).contains("passwordHash=****");
	}

	@Test
	void loginCreatesSessionAndAllowsProtectedEndpoints() throws Exception {
		MvcResult login = login("demo", "demo").andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.username").value("demo"))
			.andExpect(request().sessionAttribute("SPRING_SECURITY_CONTEXT", org.hamcrest.Matchers.notNullValue()))
			.andReturn();

		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
		assertThat(session).isNotNull();

		this.mockMvc.perform(get("/api/auth/me").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value("demo"));

		this.mockMvc
			.perform(post("/api/transcripts").session(session)
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"companyTicker": "KPITTECH", "fiscalYear": 2026, "fiscalQuarter": 4,
						 "text": "Authenticated submit."}
						"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.companyTicker").value("KPITTECH"));
	}

	@Test
	void loginIsCaseInsensitiveOnUsername() throws Exception {
		login("DEMO", "demo").andExpect(status().isOk()).andExpect(jsonPath("$.username").value("demo"));
	}

	@Test
	void rejectsUnknownUsernameWithoutRevealingIt() throws Exception {
		login("nobody", "demo").andExpect(status().isUnauthorized())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Invalid username or password."));
	}

	@Test
	void rejectsWrongPasswordWithoutRevealingUsername() throws Exception {
		login("demo", "wrong").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Invalid username or password."));
	}

	@Test
	void protectedApisRejectUnauthenticatedRequests() throws Exception {
		this.mockMvc.perform(get("/api/auth/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Authentication is required."));

		this.mockMvc
			.perform(post("/api/transcripts").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"companyTicker": "KPITTECH", "fiscalYear": 2026, "fiscalQuarter": 1, "text": "No auth"}
						"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Authentication is required."));
	}

	@Test
	void stateChangingRequestsRequireCsrf() throws Exception {
		MvcResult login = login("demo", "demo").andExpect(status().isOk()).andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

		this.mockMvc
			.perform(post("/api/transcripts").session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"companyTicker": "KPITTECH", "fiscalYear": 2026, "fiscalQuarter": 1, "text": "No CSRF"}
						"""))
			.andExpect(status().isForbidden());
	}

	@Test
	void logoutInvalidatesTheSession() throws Exception {
		MvcResult login = login("demo", "demo").andExpect(status().isOk()).andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

		this.mockMvc.perform(post("/api/auth/logout").session(session).with(csrf()))
			.andExpect(status().isNoContent());

		this.mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
	}

	@Test
	void csrfEndpointIsPublicAndReturnsToken() throws Exception {
		this.mockMvc.perform(get("/api/auth/csrf"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isNotEmpty())
			.andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"));
	}

	private ResultActions login(String username, String password) throws Exception {
		return this.mockMvc.perform(post("/api/auth/login").with(csrf())
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"username": "%s", "password": "%s"}
					""".formatted(username, password)));
	}

}
