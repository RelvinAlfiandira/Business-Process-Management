package com.uncal.bpm_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object (DTO) untuk permintaan mengganti nama (rename).
 */
@Data
public class RenameRequest {

    @NotBlank(message = "Nama baru tidak boleh kosong")
    private String newName;
}
