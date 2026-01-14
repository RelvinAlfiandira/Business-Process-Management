package com.uncal.bpm_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom Exception untuk menandakan validasi data gagal
 * (misalnya, data sudah ada di database).
 * GlobalExceptionHandler akan menangkap ini dan mengembalikan HTTP 400 BAD REQUEST.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST) // Menghasilkan HTTP 400
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
