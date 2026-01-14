package com.uncal.bpm_backend.controller;

import com.uncal.bpm_backend.model.Component;
import com.uncal.bpm_backend.repository.ComponentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/components") // Endpoint: /api/components
public class ComponentController {

    @Autowired
    private ComponentRepository componentRepository;

    /**
     * Mengambil semua komponen yang tersedia di database.
     * Endpoint: GET /api/components
     */
    @GetMapping
    public List<Component> getAllComponents() {
        // Mengembalikan daftar komponen. ID dari DB akan digunakan oleh frontend.
        return componentRepository.findAll();
    }

    /**
     * Menyimpan komponen baru yang di-upload dari frontend (Add Module).
     * Endpoint: POST /api/components
     * RequestBody sekarang menerima objek JSON untuk 'form' dan 'style' berkat JsonNode.
     */
    @PostMapping
    public ResponseEntity<?> createComponent(@RequestBody Component component) {

        // Penting: Pastikan ID disetel null agar Spring Boot melakukan operasi INSERT (Auto-increment).
        // Ini juga menetralkan potensi ID non-numeric yang dikirim dari client (seperti "sftp_sender").
        component.setId(null);

        // Tetapkan timestamp
        component.setCreatedAt(LocalDateTime.now());
        component.setUpdatedAt(LocalDateTime.now());

        try {
            Component savedComponent = componentRepository.save(component);
            // Mengembalikan 201 Created dengan data komponen yang sudah memiliki ID dari DB.
            // Data ini adalah komponen utuh yang siap digunakan di frontend.
            return new ResponseEntity<>(savedComponent, HttpStatus.CREATED);
        } catch (DataIntegrityViolationException e) {
            // Menangani error duplikasi 'label'
            return new ResponseEntity<>("Component label already exists.", HttpStatus.CONFLICT); // 409
        } catch (Exception e) {
            // Log error untuk debugging
            System.err.println("Failed to create component: " + e.getMessage());
            // Menangani error umum, termasuk masalah deserialisasi (walaupun seharusnya sudah diatasi oleh JsonNode)
            return new ResponseEntity<>("Failed to create component: " + e.getMessage(), HttpStatus.BAD_REQUEST); // 400
        }
    }

    /**
     * Menghapus komponen berdasarkan ID.
     * Endpoint: DELETE /api/components/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComponent(@PathVariable Long id) {
        try {
            if (!componentRepository.existsById(id)) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND); // 404
            }
            componentRepository.deleteById(id);
            // 204 No Content, menandakan penghapusan berhasil
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // 500
        }
    }
}
