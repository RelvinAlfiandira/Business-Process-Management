package com.uncal.bpm_backend.repository;

import com.uncal.bpm_backend.model.File;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<File, Long> {

    // Mencari file spesifik berdasarkan ID Proyek, Tipe Subfolder, dan Nama File
    Optional<File> findByProjectIdAndSubfolderTypeAndName(Long projectId, String subfolderType, String name);

    // MENCARI SEMUA FILE BERDASARKAN PROJECT ID DAN SUBFOLDER TYPE
    List<File> findByProjectIdAndSubfolderType(Long projectId, String subfolderType);

    // Menggunakan 'Project_Id' karena relasi @ManyToOne pada model File
    List<File> findByProject_Id(Long projectId);

    // Mencari file untuk fitur search
    List<File> findByProjectUserIdAndNameContainingIgnoreCase(Long projectUserId, String name);

    // Cari file by ID dengan validasi user
    @Query("SELECT f FROM File f WHERE f.id = :fileId AND f.project.user.id = :userId")
    Optional<File> findByIdAndUserId(@Param("fileId") Long fileId, @Param("userId") Long userId);
}