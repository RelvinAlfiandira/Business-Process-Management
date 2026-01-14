package com.uncal.bpm_backend.dto;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class LoginRequest {

    @NotEmpty(message = "Username tidak boleh kosong")
    private String username;

    @NotEmpty(message = "Password tidak boleh kosong")
    private String password;
}
