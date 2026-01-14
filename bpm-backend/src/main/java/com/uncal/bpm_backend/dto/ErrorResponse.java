package com.uncal.bpm_backend.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO untuk memastikan respons error API bersifat konsisten (JSON).
 */
@Data
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    // Digunakan untuk menampung error validasi field (misal: @NotBlank)
    private Map<String, String> errors;
}
