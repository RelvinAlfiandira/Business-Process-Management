package com.uncal.bpm_backend.dto;

import lombok.Data;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 * DTO untuk menerima data pembaruan profil pengguna.
 * Hanya sertakan field yang diizinkan untuk diubah.
 */
@Data
public class UserProfileUpdateDTO {

    // Kami asumsikan saat ini hanya username yang boleh diubah.
    @Size(min = 3, max = 50, message = "Username harus antara 3 hingga 50 karakter.")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username hanya boleh mengandung huruf, angka, dan underscore.")
    private String username;

    // Anda bisa menambahkan field lain di masa depan, misalnya:
    // private String email;
    // private String oldPassword;
    // private String newPassword;
}
