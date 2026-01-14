package com.uncal.bpm_backend.service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uncal.bpm_backend.dto.FileRequest;
import com.uncal.bpm_backend.dto.FileResponse;
import com.uncal.bpm_backend.dto.ProjectRequest;
import com.uncal.bpm_backend.dto.RenameRequest;
import com.uncal.bpm_backend.model.File;
import com.uncal.bpm_backend.model.Project;
import com.uncal.bpm_backend.model.User;
import com.uncal.bpm_backend.repository.FileRepository;
import com.uncal.bpm_backend.repository.ProjectRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final FileRepository fileRepository;
    private final ObjectMapper objectMapper;

    // ---------------------- PROJECT OPERATIONS ----------------------

    public List<Project> getAllProjects(User currentUser) {
        return projectRepository.findByUserId(currentUser.getId());
    }

    public Project createProject(ProjectRequest request, User currentUser) {
        Project project = new Project();
        project.setName(request.getName());
        project.setUser(currentUser);
        return projectRepository.save(project);
    }

    public void deleteProject(Long projectId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyek tidak ditemukan"));

        if (!Objects.equals(project.getUser().getId(), currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak memiliki izin untuk menghapus proyek ini");
        }

        projectRepository.delete(project);
    }

    // ---------------------- FILE OPERATIONS ----------------------

    public File saveFile(Long projectId, FileRequest request, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyek tidak ditemukan"));

        Long projectOwnerId = project.getUser().getId();
        Long currentUserId = currentUser.getId();

        log.info("SAVE FILE: Checking ownership for Project ID: {}", projectId);
        log.info("Project Owner ID (DB): {}", projectOwnerId);
        log.info("Current User ID (Token): {}", currentUserId);

        if (!Objects.equals(projectOwnerId, currentUserId)) {
            log.error("SAVE FILE FAILED: User ID {} does not match Project Owner ID {}",
                    currentUserId, projectOwnerId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak memiliki izin untuk memodifikasi proyek ini");
        }

        File file = fileRepository.findByProjectIdAndSubfolderTypeAndName(
                        projectId,
                        request.getSubfolderType(),
                        request.getName()
                )
                .orElse(new File());

        file.setProject(project);
        file.setSubfolderType(request.getSubfolderType());
        file.setName(request.getName());

        if (request.getCanvasData() != null) {
            file.setCanvasData(request.getCanvasData());
        }
        if (request.getMetadata() != null) {
            file.setMetadata(request.getMetadata());
        }

        return fileRepository.save(file);
    }

    @Transactional(readOnly = true)
    public List<FileResponse> getFilesByProjectAndSubfolder(
            Long projectId, String subfolderType, User currentUser) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyek tidak ditemukan."));

        if (!Objects.equals(project.getUser().getId(), currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak memiliki izin untuk melihat proyek ini.");
        }

        List<File> files = fileRepository.findByProjectIdAndSubfolderType(projectId, subfolderType);

        return files.stream()
                .map(FileResponse::fromModel)
                .toList();
    }

    public File getFile(Long projectId, String subfolderType, String fileName, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyek tidak ditemukan"));

        if (!Objects.equals(project.getUser().getId(), currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak memiliki izin untuk melihat proyek ini");
        }

        return fileRepository.findByProjectIdAndSubfolderTypeAndName(projectId, subfolderType, fileName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File tidak ditemukan"));
    }

    @Transactional
    public void deleteFile(Long projectId, String subfolderType, String fileName, User currentUser) {
        File file = fileRepository.findByProjectIdAndSubfolderTypeAndName(
                        projectId, subfolderType, fileName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File tidak ditemukan."));

        if (!Objects.equals(file.getProject().getUser().getId(), currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak memiliki izin untuk menghapus file ini.");
        }

        fileRepository.delete(file);
    }

    // ---------------------- CANVAS DATA OPERATIONS ----------------------

    @Transactional(readOnly = true)
    public File getFileById(Long fileId, User currentUser) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        
        if (!Objects.equals(file.getProject().getUser().getId(), currentUser.getId())) {
            throw new RuntimeException("Unauthorized to access this file");
        }
        
        return file;
    }

    @Transactional
    public void updateFileCanvasData(Long fileId, String canvasData, User currentUser) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));
        
        if (!Objects.equals(file.getProject().getUser().getId(), currentUser.getId())) {
            throw new RuntimeException("Unauthorized to access this file");
        }

        file.setCanvasData(canvasData);
        fileRepository.save(file);
    }

    // ---------------------- RENAME OPERATIONS ----------------------

    @Transactional
    public Project renameProject(Long projectId, RenameRequest request, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project tidak ditemukan."));

        if (!Objects.equals(project.getUser().getId(), currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak memiliki izin untuk mengubah project ini.");
        }

        Optional<Project> existingProject = projectRepository.findByNameAndUser(request.getNewName(), currentUser);
        if (existingProject.isPresent() && !existingProject.get().getId().equals(projectId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project dengan nama '" + request.getNewName() + "' sudah ada.");
        }

        project.setName(request.getNewName());
        project.setCreatedAt(LocalDateTime.now());
        return projectRepository.save(project);
    }

    @Transactional
    public File renameFile(Long projectId, String subfolderType, String oldFileName, RenameRequest request, User currentUser) {
        File file = fileRepository.findByProjectIdAndSubfolderTypeAndName(
                        projectId, subfolderType, oldFileName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File '" + oldFileName + "' tidak ditemukan di project ini."));

        if (!Objects.equals(file.getProject().getUser().getId(), currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak memiliki izin untuk mengubah file ini.");
        }

        String newName = request.getNewName();
        if (oldFileName.equals(newName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nama baru harus berbeda dari nama lama.");
        }

        Optional<File> existingFile = fileRepository.findByProjectIdAndSubfolderTypeAndName(
                projectId, subfolderType, newName);

        if (existingFile.isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File dengan nama '" + newName + "' sudah ada di subfolder ini.");
        }

        file.setName(newName);
        file.setCreatedAt(LocalDateTime.now());
        return fileRepository.save(file);
    }

    // ---------------------- SEARCH OPERATION ----------------------

    @Transactional(readOnly = true)
    public Map<String, List<?>> searchProjectsAndFiles(String query, User currentUser) {
        Long userId = currentUser.getId();

        List<Project> projects = projectRepository.findByUserIdAndNameContainingIgnoreCase(userId, query);
        List<File> files = fileRepository.findByProjectUserIdAndNameContainingIgnoreCase(userId, query);

        Map<String, List<?>> searchResults = new HashMap<>();
        searchResults.put("projects", projects);
        searchResults.put("files", files);

        return searchResults;
    }

    // ---------------------- NEW METHOD FOR FILE DATA UPDATE ----------------------

    @Transactional
    public void updateFileData(Long fileId, String canvasData, String metadata, User currentUser) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));

        // Check permission - pastikan user memiliki akses ke project ini
        if (!Objects.equals(file.getProject().getUser().getId(), currentUser.getId())) {
            throw new RuntimeException("Unauthorized to update this file");
        }

        // Update fields jika provided
        if (canvasData != null) {
            file.setCanvasData(canvasData);
        }
        if (metadata != null) {
            file.setMetadata(metadata);
        }

        fileRepository.save(file);
        System.out.println("✅ File data updated - ID: " + fileId);
    }

    // ---------------------- NEW METHOD FOR SCENARIO DATA SAVE ----------------------

    @Transactional
    public void saveScenarioData(Long fileId, Map<String, Object> scenarioData, User currentUser) {
        // 1. Get and validate file
        File file = getFileById(fileId, currentUser);
        
        // 2. Extract data from request
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> components = (List<Map<String, Object>>) scenarioData.get("components");
        String projectName = (String) scenarioData.get("project");
        String scenarios = (String) scenarioData.get("scenarios");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) scenarioData.get("metadata");
        
        System.out.println("🔍 Processing scenario data:");
        System.out.println("   - Components count: " + (components != null ? components.size() : 0));
        System.out.println("   - Project: " + projectName);
        System.out.println("   - Scenarios: " + scenarios);

        // 3. Format canvasData for database
        Map<String, Object> canvasDataMap = new HashMap<>();
        canvasDataMap.put("components", components);
        
        if (components != null) {
            canvasDataMap.put("flow", components.stream().map(comp -> {
                Map<String, Object> flowItem = new HashMap<>();
                flowItem.put("id", comp.get("id"));
                flowItem.put("type", comp.get("type"));
                flowItem.put("label", comp.get("label"));
                flowItem.put("config", comp.get("form") != null ? comp.get("form") : comp.get("config"));
                flowItem.put("style", comp.get("style"));
                flowItem.put("notes", comp.get("notes"));
                return flowItem;
            }).collect(Collectors.toList()));
        } else {
            canvasDataMap.put("flow", List.of());
        }
        
        canvasDataMap.put("lastModified", new Date().toString());
        canvasDataMap.put("version", 1);
        
        // 4. Format metadata for database
        Map<String, Object> fileMetadata = new HashMap<>();
        fileMetadata.put("author", currentUser.getUsername());
        fileMetadata.put("userId", currentUser.getId());
        fileMetadata.put("created", new Date().toString());
        fileMetadata.put("modified", new Date().toString());
        fileMetadata.put("componentCount", components != null ? components.size() : 0);
        fileMetadata.put("hasSender", components != null && components.stream().anyMatch(c -> "Sender".equals(c.get("type"))));
        fileMetadata.put("hasReceiver", components != null && components.stream().anyMatch(c -> "Receiver".equals(c.get("type"))));
        fileMetadata.put("hasMapping", components != null && components.stream().anyMatch(c -> "Mapping".equals(c.get("type"))));
        
        // 5. Save to scenarios API (engine) - INTERNAL CALL
        if (components != null && !components.isEmpty()) {
            saveToScenariosEngine(components, projectName, scenarios, metadata);
        }
        
        // 6. Update file in database
        try {
            updateFileData(fileId, objectMapper.writeValueAsString(canvasDataMap), 
                          objectMapper.writeValueAsString(fileMetadata), currentUser);
            System.out.println("✅ Scenario data saved successfully for file: " + fileId);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing canvas data", e);
        }
    }

    private void saveToScenariosEngine(List<Map<String, Object>> components, String projectName, 
                                      String scenarios, Map<String, Object> metadata) {
        try {
            // Format data untuk scenarios engine
            List<Map<String, Object>> engineComponents = components.stream().map(comp -> {
                Map<String, Object> engineComp = new HashMap<>();
                engineComp.put("name", comp.get("label"));
                engineComp.put("type", comp.get("type"));
                engineComp.put("uuid", comp.get("id"));
                
                // Gabungkan propname dengan form data
                Map<String, Object> attributData = new HashMap<>();
                attributData.put("propname", comp.get("label"));
                if (comp.get("form") != null) {
                    attributData.putAll((Map<String, Object>) comp.get("form"));
                } else if (comp.get("config") != null) {
                    attributData.putAll((Map<String, Object>) comp.get("config"));
                }
                engineComp.put("attribut", attributData);
                
                engineComp.put("notes", comp.get("notes"));
                engineComp.put("log", Map.of(
                    "log_type", "ALL",
                    "show_in_console", true,
                    "put_on", "File"
                ));
                engineComp.put("responsecomp", false);
                return engineComp;
            }).collect(Collectors.toList());
            
            Map<String, Object> jsonDataObject = new HashMap<>();
            jsonDataObject.put("components", engineComponents);
            jsonDataObject.put("name", scenarios);
            jsonDataObject.put("uuid", scenarios);
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("jsonData", List.of(jsonDataObject));
            payload.put("project", projectName);
            payload.put("scenarios", scenarios);
            payload.put("run_status", 0);
            payload.put("metadata", metadata != null ? metadata : new HashMap<>());
            
            // Simulate call to scenarios engine
            System.out.println("🚀 Sending to scenarios engine: " + payload);
            
        } catch (Exception e) {
            System.err.println("⚠️ Failed to save to scenarios engine: " + e.getMessage());
            // Continue with file save even if scenarios engine fails
        }
    }
}