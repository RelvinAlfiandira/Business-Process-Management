package com.uncal.bpm_backend.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FileReceiverHandler implements ComponentHandler {
    
    private final Object fileWriteLock = new Object();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private final DateTimeFormatter dateOnlyFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final DateTimeFormatter timeOnlyFormatter = DateTimeFormatter.ofPattern("HHmmss");
    
    @Override
    public String getComponentType() {
        return "Receiver";
    }
    
    @Override
    public boolean execute(ComponentExecutionData componentData, ExecutionContext context) {
        String componentLabel = componentData.getLabel();
        
        try {
            JSONObject config = componentData.getConfigData();
            String outputDirectory = config.optString("directory", "");
            
            log.info("📤 File Receiver [{}] - Output Directory: {}", componentLabel, outputDirectory);
            
            if (outputDirectory.isEmpty()) {
                log.error("❌ Invalid configuration for File Receiver: output directory is empty");
                return false;
            }
            
            if (!context.contains("fileContent")) {
                log.error("❌ No file content available in context for File Receiver");
                return false;
            }
            
            byte[] fileContent = (byte[]) context.get("fileContent");
            String originalFileName = (String) context.get("fileName");
            String sourceFilePath = (String) context.get("sourceFile");
            Long fileSize = (Long) context.get("fileSize");
            
            String processedAction = getStringFromContext(context, "processedAction", "remove");
            String renameTo = getStringFromContext(context, "renameTo", "");
            String moveTo = getStringFromContext(context, "moveTo", "");
            String sourceDirectory = getStringFromContext(context, "sourceDirectory", "");
            
            log.info("📥 Receiving file: {} ({} bytes) from: {}", originalFileName, fileSize, sourceFilePath);
            log.info("⚙️ Processing config - Action: {}, RenameTo: '{}', MoveTo: '{}'", 
                    processedAction, renameTo, moveTo);
            
            boolean transferSuccess;
            synchronized (fileWriteLock) {
                transferSuccess = processFileTransfer(fileContent, originalFileName, outputDirectory, context);
            }
            
            if (transferSuccess) {
                log.info("✅ File successfully transferred to: {}", outputDirectory);
                
                if (sourceFilePath != null && !sourceFilePath.trim().isEmpty()) {
                    log.info("🔄 Starting SOURCE FILE processing with action: {}", processedAction);
                    boolean sourceProcessed = processSourceFile(
                        sourceFilePath, 
                        processedAction, 
                        originalFileName, 
                        renameTo, 
                        moveTo,
                        sourceDirectory
                    );
                    
                    if (sourceProcessed) {
                        log.info("🎯 Source file processing COMPLETED successfully with action: {}", processedAction);
                    } else {
                        log.error("❌ Source file processing FAILED with action: {}", processedAction);
                    }
                } else {
                    log.warn("⚠️ No source file path available, skipping source file processing");
                }
                
                return true;
            } else {
                log.error("❌ File transfer failed - skipping source file processing");
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ File Receiver execution failed [{}]: {}", componentLabel, e.getMessage(), e);
            return false;
        }
    }
    
    // ✅ PERBAIKAN: Gunakan instanceof pattern matching (Java 16+)
    private String getStringFromContext(ExecutionContext context, String key, String defaultValue) {
        try {
            if (context.contains(key)) {
                Object value = context.get(key);
                if (value instanceof String stringValue) {  // ✅ Pattern matching
                    return stringValue;
                } else if (value != null) {
                    return value.toString();
                }
            }
            return defaultValue;
        } catch (Exception e) {
            log.warn("⚠️ Error getting '{}' from context, using default: {}", key, defaultValue);
            return defaultValue;
        }
    }
    
    private boolean processFileTransfer(byte[] fileContent, String originalFileName, 
                                      String outputDirectory, ExecutionContext context) {
        try {
            Path outputDir = Paths.get(outputDirectory);
            Files.createDirectories(outputDir);
            
            String outputFileName = originalFileName;
            Path outputPath = outputDir.resolve(outputFileName);
            
            log.info("💾 Writing file to destination: {}", outputPath);
            
            if (Files.exists(outputPath)) {
                log.warn("⚠️ File already exists in destination: {}, will overwrite", outputPath);
            }
            
            Files.write(outputPath, fileContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            if (Files.exists(outputPath)) {
                long writtenSize = Files.size(outputPath);
                if (writtenSize == fileContent.length) {
                    log.info("✅ File written successfully to destination: {} ({} bytes)", outputFileName, writtenSize);
                    
                    context.put("outputFile", outputPath.toString());
                    context.put("outputFileName", outputFileName);
                    context.put("outputDirectory", outputDirectory);
                    
                    return true;
                } else {
                    log.error("❌ File size mismatch: expected {} bytes, got {}", 
                             fileContent.length, writtenSize);
                    return false;
                }
            } else {
                log.error("❌ File not found after writing: {}", outputPath);
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ Error transferring file to destination: {}", e.getMessage(), e);
            return false;
        }
    }
    
    private boolean processSourceFile(String sourceFilePath, String processedAction, 
                                    String originalFileName, String renameTo, String moveTo,
                                    String sourceDirectory) {
        if (sourceFilePath == null || sourceFilePath.trim().isEmpty()) {
            log.warn("⚠️ No source file path available for processing");
            return false;
        }
        
        try {
            Path sourcePath = Paths.get(sourceFilePath);
            
            if (!Files.exists(sourcePath)) {
                log.warn("⚠️ Source file not found: {} - may have been already processed", sourceFilePath);
                return true;
            }
            
            String action = (processedAction != null && !processedAction.isEmpty()) ? 
                           processedAction.toLowerCase().trim() : "remove";
            
            log.info("🛠️ Processing SOURCE file: {} with action: '{}'", sourcePath.getFileName(), action);
            
            boolean success = false;
            
            switch (action) {
                case "remove":
                    success = performRemoveAction(sourcePath);
                    break;
                    
                case "rename":
                    success = performRenameAction(sourcePath, originalFileName, renameTo);
                    break;
                    
                case "move":
                    success = performMoveAction(sourcePath, originalFileName, moveTo, sourceDirectory);
                    break;
                    
                default:
                    log.warn("⚠️ Unknown processedAction: '{}', defaulting to remove", processedAction);
                    success = performRemoveAction(sourcePath);
                    break;
            }
            
            if (success) {
                log.info("✅ Source file processing SUCCESSFUL with action: {}", action);
            } else {
                log.error("❌ Source file processing FAILED with action: {}", action);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("❌ Error processing source file {}: {}", sourceFilePath, e.getMessage(), e);
            return false;
        }
    }
    
    private boolean performRemoveAction(Path sourcePath) {
        try {
            log.info("🗑️ REMOVING source file: {}", sourcePath);
            
            if (!Files.exists(sourcePath)) {
                log.info("✅ Source file already removed: {}", sourcePath);
                return true;
            }
            
            for (int i = 0; i < 5; i++) {
                try {
                    Files.delete(sourcePath);
                    log.info("✅ Source file REMOVED (attempt {}): {}", i + 1, sourcePath);
                    
                    if (!Files.exists(sourcePath)) {
                        log.info("✅ Source file VERIFIED AS REMOVED: {}", sourcePath);
                        return true;
                    } else {
                        log.warn("⚠️ Source file still exists after deletion attempt {}: {}", i + 1, sourcePath);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Remove attempt {} failed for {}: {}", i + 1, sourcePath, e.getMessage());
                    if (i < 4) {
                        Thread.sleep(300);
                    }
                }
            }
            
            log.error("❌ ALL REMOVE ATTEMPTS FAILED for: {}", sourcePath);
            return false;
            
        } catch (Exception e) {
            log.error("❌ Could not remove source file: {} - {}", sourcePath, e.getMessage());
            return false;
        }
    }
    
    private boolean performRenameAction(Path sourcePath, String originalFileName, String renameTo) {
        try {
            log.info("📝 RENAMING source file: {} with pattern: '{}'", sourcePath, renameTo);
            
            Path sourceDir = sourcePath.getParent();
            String newFileName = generateNewFileName(originalFileName, renameTo);
            Path newPath = sourceDir.resolve(newFileName);
            
            log.info("🔧 Renaming source: {} → {}", sourcePath.getFileName(), newFileName);
            
            if (Files.exists(newPath)) {
                log.warn("⚠️ Rename target already exists: {}, will overwrite", newPath);
            }
            
            Files.move(sourcePath, newPath, StandardCopyOption.REPLACE_EXISTING);
            
            if (Files.exists(newPath) && !Files.exists(sourcePath)) {
                log.info("✅ Source file VERIFIED AS RENAMED: {} → {}", originalFileName, newFileName);
                return true;
            } else {
                log.error("❌ Rename verification FAILED for: {}", originalFileName);
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ Could not rename source file {}: {}", sourcePath, e.getMessage());
            return false;
        }
    }
    
    private boolean performMoveAction(Path sourcePath, String originalFileName, String moveTo, String sourceDirectory) {
        try {
            log.info("📦 MOVING source file: {} to: '{}'", sourcePath, moveTo);
            
            Path targetDir;
            
            if (moveTo != null && !moveTo.trim().isEmpty()) {
                if (Paths.get(moveTo).isAbsolute()) {
                    targetDir = Paths.get(moveTo);
                    log.info("📍 Using absolute path for move: {}", targetDir);
                } else {
                    Path baseDir = (sourceDirectory != null && !sourceDirectory.isEmpty()) ? 
                                  Paths.get(sourceDirectory) : sourcePath.getParent();
                    targetDir = baseDir.resolve(moveTo);
                    log.info("📍 Using relative path for move: {} (base: {})", targetDir, baseDir);
                }
            } else {
                targetDir = sourcePath.getParent().resolve("archive");
                log.info("📍 Using default archive directory: {}", targetDir);
            }
            
            log.info("🎯 Target directory for source file: {}", targetDir);
            
            Files.createDirectories(targetDir);
            
            String newFileName = generateNewFileName(originalFileName, "");
            Path newPath = targetDir.resolve(newFileName);
            
            log.info("🚚 Moving source file: {} → {}", sourcePath.getFileName(), newPath);
            
            if (Files.exists(newPath)) {
                log.warn("⚠️ Move target already exists: {}, will overwrite", newPath);
            }
            
            Files.move(sourcePath, newPath, StandardCopyOption.REPLACE_EXISTING);
            
            if (Files.exists(newPath) && !Files.exists(sourcePath)) {
                log.info("✅ Source file VERIFIED AS MOVED: {} → {}", originalFileName, newPath);
                return true;
            } else {
                log.error("❌ Move verification FAILED for: {}", originalFileName);
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ Could not move source file {}: {}", sourcePath, e.getMessage());
            return false;
        }
    }
    
    private String generateNewFileName(String originalFileName, String renameTo) {
        if (renameTo == null || renameTo.trim().isEmpty()) {
            String timestamp = LocalDateTime.now().format(dateFormatter);
            return "processed_" + timestamp + "_" + originalFileName;
        }
        
        String extension = "";
        String fileNameWithoutExtension = originalFileName;
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFileName.substring(lastDotIndex);
            fileNameWithoutExtension = originalFileName.substring(0, lastDotIndex);
        }
        
        String newFileName = renameTo
                .replace("{timestamp}", LocalDateTime.now().format(dateFormatter))
                .replace("{original}", fileNameWithoutExtension)
                .replace("{date}", LocalDateTime.now().format(dateOnlyFormatter))
                .replace("{time}", LocalDateTime.now().format(timeOnlyFormatter))
                .replace("{datetime}", LocalDateTime.now().format(dateFormatter));
        
        if (!newFileName.contains(".") && !extension.isEmpty()) {
            newFileName += extension;
        }
        
        log.info("🔧 Generated new filename for source: '{}' from pattern: '{}'", newFileName, renameTo);
        return newFileName;
    }
}