package com.promiseledger.security;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AuthController {

	private final AuthenticationManager authenticationManager;

	private final SecurityContextRepository securityContextRepository;

	AuthController(AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository) {
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
	}

	@GetMapping("/api/auth/csrf")
	CsrfResponse csrf(CsrfToken csrfToken) {
		// CookieCsrfTokenRepository uses X-XSRF-TOKEN; the injected token's default name differs.
		return new CsrfResponse(csrfToken.getToken(), "X-XSRF-TOKEN");
	}

	@PostMapping("/api/auth/login")
	AuthUserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest,
			HttpServletResponse servletResponse) {
		try {
			Authentication authentication = this.authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.username().trim().toLowerCase(Locale.ROOT),
							request.password()));
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(authentication);
			SecurityContextHolder.setContext(context);
			this.securityContextRepository.saveContext(context, servletRequest, servletResponse);
			return new AuthUserResponse(authentication.getName());
		}
		catch (BadCredentialsException ex) {
			throw new ErrorResponseException(HttpStatus.UNAUTHORIZED,
					ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password."), ex);
		}
	}

	@GetMapping("/api/auth/me")
	AuthUserResponse me(@AuthenticationPrincipal UserDetails user) {
		if (user == null) {
			throw new ErrorResponseException(HttpStatus.UNAUTHORIZED,
					ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication is required."), null);
		}
		return new AuthUserResponse(user.getUsername());
	}

	record LoginRequest(@NotBlank(message = "is required") String username,
			@NotBlank(message = "is required") String password) {

		@Override
		public String toString() {
			return "LoginRequest[username=" + this.username + ", password=****]";
		}

	}

	record AuthUserResponse(String username) {
	}

	record CsrfResponse(String token, String headerName) {
	}

}
