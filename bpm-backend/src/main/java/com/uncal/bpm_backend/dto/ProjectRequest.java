package com.uncal.bpm_backend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ProjectRequest {
    @NotEmpty(message = "Nama proyek tidak boleh kosong")
    private String name;
}
