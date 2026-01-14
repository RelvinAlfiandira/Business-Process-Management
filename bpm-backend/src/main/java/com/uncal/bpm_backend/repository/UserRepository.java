package com.uncal.bpm_backend.repository;

import com.uncal.bpm_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Method kustom untuk mencari user berdasarkan username (sudah ada)
    Optional<User> findByUsername(String username);

    // Method kustom untuk mencari user berdasarkan email (sudah ada)
    Optional<User> findByEmail(String email);

    /**
     * PENTING: Mengecek apakah username sudah ada,
     * MENGECUALIKAN pengguna dengan ID tertentu.
     * Ini digunakan saat UPDATE profil agar user dapat mempertahankan username-nya sendiri.
     */
    boolean existsByUsernameAndIdNot(String username, Long id); // <-- INI YANG DITAMBAHKAN
}