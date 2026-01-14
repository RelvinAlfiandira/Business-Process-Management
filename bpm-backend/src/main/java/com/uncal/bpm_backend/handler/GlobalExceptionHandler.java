package com.uncal.bpm_backend.handler;

import com.uncal.bpm_backend.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Kelas yang bertugas menangani semua pengecualian (exceptions) yang dilempar
 * dari Controller atau Service, dan mengembalikannya dalam format JSON yang seragam.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Menangani ResponseStatusException (misal: 404 Not Found, 403 Forbidden yang dilempar dari Service).
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request) {

        HttpStatus status = (HttpStatus) ex.getStatusCode();

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(ex.getReason())
                .path(request.getRequestURI())
                .build();

        log.warn("ResponseStatusException caught: {} - {}", status.value(), ex.getReason());
        return new ResponseEntity<>(response, status);
    }

    /**
     * Menangani MethodArgumentNotValidException (Error Validasi @Valid).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        HttpStatus status = HttpStatus.BAD_REQUEST;

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message("Validasi gagal untuk beberapa field.")
                .path(request.getRequestURI())
                .errors(errors)
                .build();

        log.warn("Validation Exception caught for path {}: {}", request.getRequestURI(), errors);
        return new ResponseEntity<>(response, status);
    }

    /**
     * Menangani Exception umum (Error 500 Internal Server Error).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message("Terjadi kesalahan internal server yang tidak terduga.")
                .path(request.getRequestURI())
                .build();

        log.error("Internal Server Error caught:", ex); // Log error penuh
        return new ResponseEntity<>(response, status);
    }
}
