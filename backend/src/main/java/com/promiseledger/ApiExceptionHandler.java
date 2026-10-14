package com.promiseledger;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<InvalidField> errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(fieldError -> new InvalidField(fieldError.getField(),
					Objects.requireNonNullElse(fieldError.getDefaultMessage(), "is invalid")))
			.sorted(Comparator.comparing(InvalidField::field))
			.toList();
		ProblemDetail problem = ex.getBody();
		problem.setDetail("One or more fields are invalid.");
		problem.setProperty("errors", errors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status,
				"Request body is missing or is not valid JSON with the expected field types.");
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	record InvalidField(String field, String message) {
	}

}
