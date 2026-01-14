package com.uncal.bpm_backend.service;

import com.uncal.bpm_backend.model.User;
import com.uncal.bpm_backend.repository.UserRepository;
import com.uncal.bpm_backend.dto.UserProfileUpdateDTO;
import com.uncal.bpm_backend.exception.ResourceNotFoundException;
import com.uncal.bpm_backend.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;


    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan dengan username: " + username));
    }

    /**
     * Memperbarui profil pengguna yang sedang login.
     * @param currentUsername Username pengguna yang sedang login (sebagai kunci pencarian).
     * @param updateDto DTO yang berisi data yang akan diperbarui.
     * @return Objek User yang telah diperbarui.
     * @throws ResourceNotFoundException Jika user tidak ditemukan.
     * @throws ValidationException Jika username yang baru sudah digunakan.
     */
    @Transactional
    public User updateUser(String currentUsername, UserProfileUpdateDTO updateDto) {
        // 1. Ambil User saat ini
        User userToUpdate = findByUsername(currentUsername);

        // 2. Proses pembaruan username (jika DTO memiliki nilai)
        if (updateDto.getUsername() != null && !updateDto.getUsername().isBlank()) {
            String newUsername = updateDto.getUsername();

            // Cek apakah username baru berbeda dari yang lama
            if (!userToUpdate.getUsername().equals(newUsername)) {

                // Cek keunikan di database, kecuali userToUpdate.getId()
                if (userRepository.existsByUsernameAndIdNot(newUsername, userToUpdate.getId())) {
                    throw new ValidationException("Username '" + newUsername + "' sudah digunakan oleh pengguna lain.");
                }

                // Terapkan username baru
                userToUpdate.setUsername(newUsername);
            }
        }

        // Catatan: Jika ada field lain yang bisa diupdate (misalnya email),
        // tambahkan logika pengecekan di sini.

        // 3. Simpan perubahan ke database
        return userRepository.save(userToUpdate);
    }
}
