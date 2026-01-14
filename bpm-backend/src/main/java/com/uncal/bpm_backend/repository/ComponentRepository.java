package com.uncal.bpm_backend.repository;

import com.uncal.bpm_backend.model.Component;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository untuk Component Entity, menangani komunikasi ke PostgreSQL.
 */
@Repository
public interface ComponentRepository extends JpaRepository<Component, Long> {
    // Semua operasi CRUD (Save, FindAll, Delete, dll) otomatis tersedia
}
