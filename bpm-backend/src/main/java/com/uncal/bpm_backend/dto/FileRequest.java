package com.uncal.bpm_backend.dto;


import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class FileRequest {
    @NotEmpty(message = "Nama file tidak boleh kosong")
    private String name;

    @NotEmpty(message = "Tipe subfolder tidak boleh kosong")
    private String subfolderType; // Contoh: 'scenarios', 'mappings', 'java'

    // Data JSON untuk flow components (dapat berupa string JSON)
    private String canvasData;

    // Data JSON untuk konfigurasi/metadata (dapat berupa string JSON)
    private String metadata;
}
