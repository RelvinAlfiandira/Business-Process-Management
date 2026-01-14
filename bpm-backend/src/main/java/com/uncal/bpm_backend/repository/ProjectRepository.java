package com.uncal.bpm_backend.repository;

import com.uncal.bpm_backend.model.Project;
import com.uncal.bpm_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Mencari semua proyek berdasarkan ID pengguna (user_id)
    List<Project> findByUserId(Long userId);

    /**
     * Mencari Project berdasarkan nama dan User pemiliknya.
     * Digunakan untuk memastikan nama Project baru unik untuk User ini.
     * Asumsi: field relasi di Project.java bernama 'user'.
     */
    Optional<Project> findByNameAndUser(String name, User user); // <-- BARU: Method ini yang dibutuhkan ProjectService

    List<Project> findByUserIdAndNameContainingIgnoreCase(Long userId, String name);

}
