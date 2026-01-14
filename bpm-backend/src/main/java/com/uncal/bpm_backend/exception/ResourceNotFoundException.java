package com.uncal.bpm_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom Exception untuk menandakan resource (sumber daya) tidak ditemukan.
 * GlobalExceptionHandler akan menangkap ini dan mengembalikan HTTP 404 NOT FOUND.
 */
@ResponseStatus(HttpStatus.NOT_FOUND) // Menghasilkan HTTP 404
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}