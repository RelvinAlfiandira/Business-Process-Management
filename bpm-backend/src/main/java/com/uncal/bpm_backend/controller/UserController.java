package com.uncal.bpm_backend.controller;

import com.uncal.bpm_backend.model.User;
import com.uncal.bpm_backend.service.UserService;
import com.uncal.bpm_backend.dto.UserProfileUpdateDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Endpoint untuk mengambil detail pengguna yang sedang login.
     * Menggunakan Principal dari SecurityContextHolder untuk mendapatkan nama pengguna.
     * * @return Detail objek User (tanpa passwordHash)
     */
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser() {
        // Ambil objek Authentication dari Security Context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Nama pengguna (username) adalah Principal di sini setelah berhasil login/autentikasi JWT
        String username = authentication.getName();

        // Panggil service untuk mencari User berdasarkan username
        User currentUser = userService.findByUsername(username);

        // Catatan: Model User sudah dilindungi dengan @JsonIgnore pada List<Project>
        // sehingga ini harusnya sukses.
        return ResponseEntity.ok(currentUser);
    }

    /**
     * Endpoint untuk memperbarui profil pengguna (saat ini hanya mendukung username).
     * * @param updateDto DTO yang berisi data yang akan diperbarui.
     * @return Detail objek User yang telah diperbarui.
     */
    @PatchMapping("/me")
    public ResponseEntity<User> updateCurrentUser(@Valid @RequestBody UserProfileUpdateDTO updateDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        User updatedUser = userService.updateUser(currentUsername, updateDto);

        return ResponseEntity.ok(updatedUser);
    }
}
