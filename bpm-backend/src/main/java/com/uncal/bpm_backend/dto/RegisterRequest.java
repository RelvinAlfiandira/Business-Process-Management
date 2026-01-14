package com.uncal.bpm_backend.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotEmpty(message = "Username tidak boleh kosong")
    @Size(min = 3, max = 50, message = "Username harus antara 3 dan 50 karakter")
    private String username;

    @NotEmpty(message = "Password tidak boleh kosong")
    @Size(min = 6, message = "Password minimal 6 karakter")
    private String password;

    @Email(message = "Format email tidak valid")
    private String email;
}
