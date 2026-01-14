package com.uncal.bpm_backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uncal.bpm_backend.dto.FileRequest;
import com.uncal.bpm_backend.dto.FileResponse;
import com.uncal.bpm_backend.dto.ProjectRequest;
import com.uncal.bpm_backend.dto.RenameRequest;
import com.uncal.bpm_backend.dto.SearchResponse;
import com.uncal.bpm_backend.model.File;
import com.uncal.bpm_backend.model.Project;
import com.uncal.bpm_backend.model.User;
import com.uncal.bpm_backend.service.ProjectService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    // GET /api/projects : Mengambil semua proyek user
    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects(@AuthenticationPrincipal User currentUser) {
        List<Project> projects = projectService.getAllProjects(currentUser);
        return ResponseEntity.ok(projects);
    }

    // POST /api/projects : Membuat proyek baru
    @PostMapping
    public ResponseEntity<Project> createProject(
            @Valid @RequestBody ProjectRequest request,
            @AuthenticationPrincipal User currentUser) {
        Project newProject = projectService.createProject(request, currentUser);
        return new ResponseEntity<>(newProject, HttpStatus.CREATED);
    }

    // DELETE /api/projects/{projectId} : Menghapus proyek
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User currentUser) {
        projectService.deleteProject(projectId, currentUser);
        return ResponseEntity.noContent().build();
    }

    // PATCH /api/projects/{projectId}/rename : Mengganti nama project
    @PatchMapping("/{projectId}/rename")
    public ResponseEntity<Project> renameProject(
            @PathVariable Long projectId,
            @Valid @RequestBody RenameRequest request,
            @AuthenticationPrincipal User currentUser) {
        Project updatedProject = projectService.renameProject(projectId, request, currentUser);
        return ResponseEntity.ok(updatedProject);
    }

    // ---------------------- FILE ENDPOINTS ----------------------

    // POST /api/projects/{projectId}/files : Menyimpan atau memperbarui file
    @PostMapping("/{projectId}/files")
    public ResponseEntity<?> saveFile(
            @PathVariable Long projectId,
            @Valid @RequestBody FileRequest request,
            @AuthenticationPrincipal User currentUser) {
        projectService.saveFile(projectId, request, currentUser);
        return ResponseEntity.ok("File berhasil disimpan/diperbarui");
    }

    /**
     * GET /api/projects/{projectId}/files/{subfolderType}
     * Mengambil daftar file (Scenarios/Mappings/dll.) untuk Subfolder tertentu.
     */
    @GetMapping("/{projectId}/files/{subfolderType}")
    public ResponseEntity<List<FileResponse>> getFilesBySubfolder(
            @PathVariable Long projectId,
            @PathVariable String subfolderType,
            @AuthenticationPrincipal User currentUser) {

        List<FileResponse> files = projectService.getFilesByProjectAndSubfolder(
                projectId, subfolderType, currentUser);
        return ResponseEntity.ok(files);
    }

    // GET /api/projects/{projectId}/files/{subfolderType}/{fileName} : Mengambil detail file
    @GetMapping("/{projectId}/files/{subfolderType}/{fileName}")
    public ResponseEntity<FileResponse> getFile(
            @PathVariable Long projectId,
            @PathVariable String subfolderType,
            @PathVariable String fileName,
            @AuthenticationPrincipal User currentUser) {
        File file = projectService.getFile(projectId, subfolderType, fileName, currentUser);
        return ResponseEntity.ok(FileResponse.fromModel(file));
    }

    // PATCH /api/projects/{projectId}/files/{subfolderType}/{fileName}/rename : Mengganti nama file
    @PatchMapping("/{projectId}/files/{subfolderType}/{fileName}/rename")
    public ResponseEntity<FileResponse> renameFile(
            @PathVariable Long projectId,
            @PathVariable String subfolderType,
            @PathVariable String fileName,
            @Valid @RequestBody RenameRequest request,
            @AuthenticationPrincipal User currentUser) {

        File updatedFile = projectService.renameFile(projectId, subfolderType, fileName, request, currentUser);
        return ResponseEntity.ok(FileResponse.fromModel(updatedFile));
    }

    @DeleteMapping("/{projectId}/files/{subfolderType}/{fileName}")
    public ResponseEntity<Void> deleteFile(
            @PathVariable Long projectId,
            @PathVariable String subfolderType,
            @PathVariable String fileName,
            @AuthenticationPrincipal User currentUser) {

        projectService.deleteFile(projectId, subfolderType, fileName, currentUser);
        return ResponseEntity.noContent().build();
    }

    // ---------------------- CANVAS DATA ENDPOINTS ----------------------

    // GET /api/projects/files/{fileId}/canvas - Get canvas data untuk file
    @GetMapping("/files/{fileId}/canvas")
    public ResponseEntity<?> getFileCanvasData(
            @PathVariable Long fileId,
            @AuthenticationPrincipal User currentUser) {
        try {
            File file = projectService.getFileById(fileId, currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("id", file.getId());
            response.put("name", file.getName());
            response.put("subfolderType", file.getSubfolderType());
            response.put("canvasData", file.getCanvasData());
            response.put("metadata", file.getMetadata());
            response.put("runStatus", file.getRunStatus());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/projects/files/canvas/{fileId} - Update canvas data saja
    @PutMapping("/files/canvas/{fileId}")
    public ResponseEntity<?> updateFileCanvasData(
            @PathVariable Long fileId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User currentUser) {
        try {
            System.out.println("🎯 UPDATE CANVAS - File ID: " + fileId);
            System.out.println("🎯 UPDATE CANVAS - User: " + currentUser.getUsername());

            String canvasData = request.get("canvasData");
            System.out.println("🎯 UPDATE CANVAS - Data length: " + (canvasData != null ? canvasData.length() : "NULL"));

            projectService.updateFileCanvasData(fileId, canvasData, currentUser);

            System.out.println("✅ UPDATE CANVAS - Success for file: " + fileId);
            return ResponseEntity.ok().body(Map.of("message", "Canvas data updated successfully"));
        } catch (Exception e) {
            System.err.println("❌ UPDATE CANVAS - Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/projects/files/{fileId} - Update file data termasuk canvas_data dan metadata
    @PutMapping("/files/{fileId}")
    public ResponseEntity<?> updateFileData(
            @PathVariable Long fileId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal User currentUser) {
        try {
            System.out.println("🎯 UPDATE FILE DATA - File ID: " + fileId);
            System.out.println("🎯 UPDATE FILE DATA - User: " + currentUser.getUsername());

            // Extract data dari request
            String canvasData = request.get("canvasData") != null ? 
                request.get("canvasData").toString() : null;
            String metadata = request.get("metadata") != null ? 
                request.get("metadata").toString() : null;

            System.out.println("🎯 UPDATE FILE DATA - Canvas Data: " + (canvasData != null ? "Provided" : "NULL"));
            System.out.println("🎯 UPDATE FILE DATA - Metadata: " + (metadata != null ? "Provided" : "NULL"));

            // Update file data
            projectService.updateFileData(fileId, canvasData, metadata, currentUser);

            System.out.println("✅ UPDATE FILE DATA - Success for file: " + fileId);
            return ResponseEntity.ok().body(Map.of("message", "File data updated successfully"));
        } catch (Exception e) {
            System.err.println("❌ UPDATE FILE DATA - Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/projects/files/{fileId}/save-scenario - Save complete scenario data
    @PostMapping("/files/{fileId}/save-scenario")
    public ResponseEntity<?> saveScenarioData(
            @PathVariable Long fileId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal User currentUser) {
        try {
            System.out.println("🎯 SAVE SCENARIO - File ID: " + fileId);
            System.out.println("🎯 SAVE SCENARIO - User: " + currentUser.getUsername());

            // Process and save scenario data
            projectService.saveScenarioData(fileId, request, currentUser);

            System.out.println("✅ SAVE SCENARIO - Success for file: " + fileId);
            return ResponseEntity.ok().body(Map.of("message", "Scenario data saved successfully"));
        } catch (Exception e) {
            System.err.println("❌ SAVE SCENARIO - Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ---------------------- SEARCH ENDPOINT ----------------------

    // GET /api/projects/search?q={query} : Mencari project dan file
    @GetMapping("/search")
    public ResponseEntity<SearchResponse> searchProjects(
            @RequestParam(name = "q") String query,
            @AuthenticationPrincipal User currentUser) {

        Map<String, List<?>> results = projectService.searchProjectsAndFiles(query, currentUser);

        SearchResponse response = SearchResponse.builder()
                .projects((List<Project>) results.get("projects"))
                .files((List<File>) results.get("files"))
                .build();

        return ResponseEntity.ok(response);
    }
}