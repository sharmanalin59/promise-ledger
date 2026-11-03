package com.promiseledger.security;

import java.io.IOException;
import java.util.function.Supplier;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JsonMapper jsonMapper) throws Exception {
		CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		csrfTokenRepository.setHeaderName("X-XSRF-TOKEN");
		csrfTokenRepository.setCookieCustomizer((cookie) -> cookie.path("/").sameSite("Lax"));

		http.csrf((csrf) -> csrf.csrfTokenRepository(csrfTokenRepository)
			.csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
			.sessionManagement((sessions) -> sessions.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
				.sessionFixation((fixation) -> fixation.changeSessionId()))
			.securityContext((context) -> context.securityContextRepository(securityContextRepository()))
			.authorizeHttpRequests((auth) -> auth.requestMatchers(HttpMethod.POST, "/api/auth/login")
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/api/auth/csrf")
				.permitAll()
				.requestMatchers("/error")
				.permitAll()
				.anyRequest()
				.authenticated())
			.exceptionHandling((exceptions) -> exceptions
				.authenticationEntryPoint((request, response, ex) -> writeProblem(response, jsonMapper,
						HttpStatus.UNAUTHORIZED, "Authentication is required."))
				.accessDeniedHandler((request, response, ex) -> writeProblem(response, jsonMapper, HttpStatus.FORBIDDEN,
						"Access is denied.")))
			.logout((logout) -> logout.logoutUrl("/api/auth/logout")
				.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
				.deleteCookies("JSESSIONID", "XSRF-TOKEN")
				.invalidateHttpSession(true)
				.clearAuthentication(true))
			.httpBasic((basic) -> basic.disable())
			.formLogin((form) -> form.disable())
			.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class);
		return http.build();
	}

	private static void writeProblem(HttpServletResponse response, JsonMapper jsonMapper, HttpStatus status,
			String detail) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		jsonMapper.writeValue(response.getOutputStream(), ProblemDetail.forStatusAndDetail(status, detail));
	}

	static final class CsrfCookieFilter extends OncePerRequestFilter {

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
				throws ServletException, IOException {
			CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
			if (csrfToken != null) {
				csrfToken.getToken();
			}
			filterChain.doFilter(request, response);
		}

	}

	static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

		private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();

		private final XorCsrfTokenRequestAttributeHandler xor = new XorCsrfTokenRequestAttributeHandler();

		@Override
		public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
			this.xor.handle(request, response, csrfToken);
		}

		@Override
		public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
			String headerValue = request.getHeader(csrfToken.getHeaderName());
			return (headerValue != null && !headerValue.isBlank())
					? this.plain.resolveCsrfTokenValue(request, csrfToken)
					: this.xor.resolveCsrfTokenValue(request, csrfToken);
		}

	}

}
