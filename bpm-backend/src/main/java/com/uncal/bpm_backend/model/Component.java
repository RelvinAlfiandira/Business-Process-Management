package com.uncal.bpm_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

// Import Jackson untuk penanganan JSON Objects secara native
import com.fasterxml.jackson.databind.JsonNode;

// Import untuk penanganan JSONB di PostgreSQL
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Representasi dari komponen yang disimpan di Palette.
 * Menggunakan tipe data JSONB untuk field 'form' dan 'style' yang dimapping ke JsonNode di Java.
 */
@Entity
@Table(name = "component", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"label"}) // Memastikan nama komponen unik
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Component {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String label; // Nama yang ditampilkan di Palette

    @Column(nullable = false, length = 50)
    private String type; // Kategori (e.g., "Sender", "Receiver")

    // Menggunakan TEXT untuk Base64 Icon yang panjang
    @Column(columnDefinition = "TEXT")
    private String icon;

    // FIX: Menggunakan JsonNode agar Jackson bisa menerima JSON Object langsung dari payload.
    // Anotasi Hibernate akan tetap memastikan JsonNode ini diubah menjadi JSONB di DB.
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "form", columnDefinition = "jsonb", nullable = false)
    private JsonNode form; // FIX: Diubah dari String ke JsonNode

    // FIX: Menggunakan JsonNode
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "style", columnDefinition = "jsonb")
    private JsonNode style; // FIX: Diubah dari String ke JsonNode

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
