package com.uncal.bpm_backend.service;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FileSenderHandler implements ComponentHandler {
    
    private final ConcurrentHashMap<String, Long> processedFiles = new ConcurrentHashMap<>();
    private static final long PROCESSED_FILE_TTL = 5 * 60 * 1000;
    private final Object directoryLock = new Object();
    
    @Override
    public String getComponentType() {
        return "Sender";
    }
    
    @Override
    public boolean execute(ComponentExecutionData componentData, ExecutionContext context) {
        String componentLabel = componentData.getLabel();
        
        try {
            JSONObject config = componentData.getConfigData();
            String directory = config.optString("directory", "");
            String filePattern = config.optString("filePattern", "*");
            String encoding = config.optString("encoding", "UTF-8");
            String processedAction = config.optString("processedAction", "remove");
            
            // ✅ Ambil renameTo dan moveTo dari konfigurasi JSON
            String renameTo = config.optString("renameTo", "");
            String moveTo = config.optString("moveTo", "");
            
            log.info("📁 File Sender [{}] - Directory: {}, Pattern: {}, Action: {}, RenameTo: {}, MoveTo: {}", 
                    componentLabel, directory, filePattern, processedAction, renameTo, moveTo);
            
            if (directory.isEmpty()) {
                log.error("❌ Invalid configuration for File Sender: directory is empty");
                return false;
            }
            
            synchronized (directoryLock) {
                List<java.io.File> files = findMatchingFiles(directory, filePattern);
                
                List<java.io.File> newFiles = files.stream()
                        .filter(file -> !isAlreadyProcessed(file))
                        .collect(Collectors.toList());
                
                if (newFiles.isEmpty()) {
                    log.info("📭 No new files found matching pattern: {} in directory: {}", filePattern, directory);
                    cleanupProcessedFiles();
                    return false;
                }
                
                java.io.File file = newFiles.get(0);
                log.info("📄 Reading file: {} ({} bytes)", file.getName(), file.length());
                
                byte[] fileContent = readFileAsBytes(file);
                
                // ✅ PERBAIKAN: HANYA pass konfigurasi ke context - TIDAK memproses file sumber di sini
                context.put("sourceFile", file.getAbsolutePath());
                context.put("fileName", file.getName());
                context.put("fileSize", file.length());
                context.put("fileContent", fileContent);
                context.put("fileEncoding", encoding);
                context.put("processedAction", processedAction);
                context.put("renameTo", renameTo);
                context.put("moveTo", moveTo);
                context.put("sourceDirectory", directory);
                
                // ✅ Tandai sebagai processed TANPA memodifikasi file sumber
                markAsProcessed(file);
                
                log.info("✅ File read successfully: {} ({} bytes) - Processing action '{}' will be handled by Receiver", 
                        file.getName(), file.length(), processedAction);
                return true;
            }
            
        } catch (Exception e) {
            log.error("❌ File Sender execution failed [{}]: {}", componentLabel, e.getMessage(), e);
            return false;
        }
    }
    
    private List<java.io.File> findMatchingFiles(String directoryPath, String filePattern) {
        try {
            Path dir = Paths.get(directoryPath);
            if (!Files.exists(dir) || !Files.isDirectory(dir)) {
                log.error("❌ Directory does not exist: {}", directoryPath);
                return Collections.emptyList();
            }
            
            final java.nio.file.PathMatcher pathMatcher;
            if (filePattern.equals("*")) {
                pathMatcher = FileSystems.getDefault().getPathMatcher("glob:*");
            } else {
                pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + filePattern);
            }
            
            return Files.list(dir)
                    .filter(Files::isRegularFile)
                    .filter(path -> pathMatcher.matches(path.getFileName()))
                    .map(Path::toFile)
                    .sorted((f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()))
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("❌ Error finding matching files in {}: {}", directoryPath, e.getMessage());
            return Collections.emptyList();
        }
    }
    
    private byte[] readFileAsBytes(java.io.File file) {
        try {
            if (!isFileStable(file)) {
                throw new RuntimeException("File is still being written or unstable: " + file.getName());
            }
            
            return Files.readAllBytes(file.toPath());
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + file.getName(), e);
        }
    }
    
    private boolean isFileStable(java.io.File file) {
        try {
            long size1 = file.length();
            Thread.sleep(50);
            long size2 = file.length();
            Thread.sleep(50);
            long size3 = file.length();
            
            boolean isStable = (size1 == size2) && (size2 == size3);
            if (!isStable) {
                log.warn("⚠️ File is unstable: {} (sizes: {}, {}, {})", 
                        file.getName(), size1, size2, size3);
            }
            return isStable;
        } catch (Exception e) {
            log.warn("⚠️ Error checking file stability: {}", file.getName(), e);
            return false;
        }
    }
    
    private boolean isAlreadyProcessed(java.io.File file) {
        String fileKey = file.getAbsolutePath();
        Long processedTime = processedFiles.get(fileKey);
        
        if (processedTime != null) {
            if (System.currentTimeMillis() - processedTime < PROCESSED_FILE_TTL) {
                log.debug("⏭️ Skipping already processed file: {}", file.getName());
                return true;
            } else {
                processedFiles.remove(fileKey);
            }
        }
        return false;
    }
    
    private void markAsProcessed(java.io.File file) {
        String fileKey = file.getAbsolutePath();
        processedFiles.put(fileKey, System.currentTimeMillis());
        log.debug("🔖 Marked file as processed: {}", file.getName());
    }
    
    private void cleanupProcessedFiles() {
        long currentTime = System.currentTimeMillis();
        processedFiles.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > PROCESSED_FILE_TTL
        );
    }
    
    public void clearProcessedFiles() {
        processedFiles.clear();
        log.info("🧹 Cleared processed files cache");
    }
}