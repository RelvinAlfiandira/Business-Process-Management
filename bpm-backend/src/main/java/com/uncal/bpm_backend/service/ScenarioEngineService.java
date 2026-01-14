package com.uncal.bpm_backend.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uncal.bpm_backend.model.ExecutionLog;
import com.uncal.bpm_backend.model.File;
import com.uncal.bpm_backend.model.User;
import com.uncal.bpm_backend.repository.FileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScenarioEngineService {

    private final FileRepository fileRepository;
    private final ComponentExecutorService componentExecutor;
    private final TaskScheduler taskScheduler;
    private final ExecutionLogService executionLogService;
    
    private final Map<Long, Boolean> runningScenarios = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @Transactional
    public void runScenario(Long fileId, User currentUser) {
        try {
            log.info("🚀 RUN SCENARIO - File ID: {}, User: {}", 
                    fileId, currentUser != null ? currentUser.getUsername() : "null");
            
            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found with ID: " + fileId));
            
            log.info("✅ File found: {} (Type: {})", file.getName(), file.getSubfolderType());
            
            if (!"scenarios".equals(file.getSubfolderType())) {
                throw new RuntimeException("Only scenario files can be executed. Type: " + file.getSubfolderType());
            }
            
            log.info("📝 Updating run status to 1 for scenario: {}", file.getName());
            
            String currentCanvasData = file.getCanvasData();
            if (currentCanvasData == null || !currentCanvasData.contains("runStatus")) {
                log.warn("⚠️ runStatus not found in canvas_data, adding it...");
            }
            
            file.setRunStatus(1);
            File savedFile = fileRepository.save(file);
            
            log.info("✅ Run status updated to: {}", savedFile.getRunStatus());
            
            runningScenarios.put(fileId, true);
            startScheduledExecution(file);
            
            log.info("🎯 Scenario {} STARTED successfully", file.getName());
            
        } catch (Exception e) {
            log.error("💥 Failed to run scenario: {}", e.getMessage(), e);
            throw new RuntimeException("Run scenario failed: " + e.getMessage(), e);
        }
    }

    @Transactional  
    public void stopScenario(Long fileId, User currentUser) {
        try {
            log.info("🛑 STOP SCENARIO - File ID: {}, User: {}", 
                    fileId, currentUser != null ? currentUser.getUsername() : "null");
            
            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found"));
            
            log.info("📝 Updating run status to 0 for scenario: {}", file.getName());
            file.setRunStatus(0);
            fileRepository.save(file);
            
            stopScheduledExecution(fileId);
            runningScenarios.remove(fileId);
            
            log.info("🎯 Scenario {} STOPPED successfully", file.getName());
            
        } catch (Exception e) {
            log.error("💥 Failed to stop scenario: {}", e.getMessage(), e);
            throw new RuntimeException("Stop scenario failed: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Integer getScenarioStatus(Long fileId, User currentUser) {
        try {
            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found"));
            
            Integer status = file.getRunStatus();
            log.debug("📊 Scenario status for {}: {}", file.getName(), status);
            return status;
            
        } catch (Exception e) {
            log.error("❌ Error getting scenario status for file {}: {}", fileId, e.getMessage());
            throw new RuntimeException("Failed to get scenario status: " + e.getMessage(), e);
        }
    }

    private void startScheduledExecution(File file) {
        try {
            log.info("⏰ Starting scheduled execution for: {}", file.getName());
            
            if (file.getCanvasData() == null || file.getCanvasData().trim().isEmpty()) {
                log.warn("⚠️ Canvas data is empty for file: {}, will execute once", file.getName());
                executeScenario(file);
                return;
            }
            
            JSONObject canvasData = parseCanvasData(file.getCanvasData());
            Integer pollingInterval = extractPollingInterval(canvasData);
            
            log.info("🔄 Scheduling execution with interval: {} seconds", pollingInterval);
            
            ScheduledFuture<?> future = taskScheduler.scheduleWithFixedDelay(
                () -> executeScenario(file),
                Duration.ofSeconds(pollingInterval != null ? pollingInterval : 60)
            );
            
            scheduledTasks.put(file.getId(), future);
            
            log.info("✅ Scheduled execution started for scenario: {} (interval: {}s)", 
                    file.getName(), pollingInterval);
            
        } catch (Exception e) {
            log.error("❌ Failed to start scheduled execution for scenario {}: {}", 
                     file.getName(), e.getMessage());
            log.info("🔄 Falling back to immediate execution");
            executeScenario(file);
        }
    }

    private void stopScheduledExecution(Long fileId) {
        try {
            ScheduledFuture<?> future = scheduledTasks.get(fileId);
            if (future != null) {
                future.cancel(false);
                scheduledTasks.remove(fileId);
                log.info("✅ Cancelled scheduled execution for file: {}", fileId);
            } else {
                log.warn("⚠️ No scheduled execution found for file: {}", fileId);
            }
        } catch (Exception e) {
            log.error("❌ Error stopping scheduled execution for file {}: {}", fileId, e.getMessage());
        }
    }

    private void executeScenario(File file) {
        Long scenarioId = file.getId();
        String scenarioName = file.getName();
        
        Integer currentRunStatus = file.getRunStatus();
        boolean shouldRun = currentRunStatus != null && currentRunStatus == 1;
        
        if (!shouldRun) {
            log.debug("⏸️ Scenario {} is stopped (run_status: {}), skipping execution", 
                     scenarioName, currentRunStatus);
            
            if (runningScenarios.getOrDefault(scenarioId, false)) {
                runningScenarios.remove(scenarioId);
                stopScheduledExecution(scenarioId);
            }
            return;
        }
        
        runningScenarios.put(scenarioId, true);
        
        // ✅ Declare variables outside try block for proper scope
        String executionId = UUID.randomUUID().toString();
        Long logId = null;
        boolean logCreated = false;
        
        try {
            log.info("🔄 Starting FILE TRANSFER scenario: {} (run_status: {})", 
                    scenarioName, currentRunStatus);
            
            JSONObject canvasData = parseCanvasData(file.getCanvasData());
            List<ComponentExecutionData> components = extractExecutionComponents(canvasData);
            
            if (components.isEmpty()) {
                log.warn("⚠️ No executable components found in scenario: {}", scenarioName);
                return;
            }
            
            boolean hasSender = components.stream().anyMatch(c -> "Sender".equals(c.getType()));
            boolean hasReceiver = components.stream().anyMatch(c -> "Receiver".equals(c.getType()));
            
            if (!hasSender || !hasReceiver) {
                log.error("❌ Invalid scenario: Must have both Sender and Receiver components");
                return;
            }
            
            ExecutionContext context = new ExecutionContext();
            context.put("scenarioId", scenarioId);
            context.put("scenarioName", scenarioName);
            context.put("executionTime", LocalDateTime.now());
            context.put("executionId", executionId);
            
            log.info("📋 Executing {} components for file transfer", components.size());
            
            boolean overallSuccess = true;
            
            for (int i = 0; i < components.size(); i++) {
                ComponentExecutionData componentData = components.get(i);
                
                log.info("⚡ [{}/{}] Executing: {} - {}", 
                        i + 1, components.size(), componentData.getType(), componentData.getLabel());
                
                // ✅ PERBAIKAN: Pass seluruh config data ke context untuk processedAction
                if ("Sender".equals(componentData.getType())) {
                    String processedAction = componentData.getConfigData().optString("processedAction", "remove");
                    String renameTo = componentData.getConfigData().optString("renameTo", "");
                    String moveTo = componentData.getConfigData().optString("moveTo", "");
                    
                    log.info("🔧 Sender config - processedAction: {}, renameTo: {}, moveTo: {}", 
                            processedAction, renameTo, moveTo);
                    
                    // Simpan di context untuk digunakan Receiver
                    context.put("processedAction", processedAction);
                    context.put("renameTo", renameTo);
                    context.put("moveTo", moveTo);
                }
                
                // ✅ PERBAIKAN: Gunakan executeComponentWithRetry untuk retry mechanism
                boolean componentSuccess = componentExecutor.executeComponentWithRetry(componentData, context);
                
                if (componentSuccess) {
                    // ✅ Create execution log when Sender successfully processes data
                    if ("Sender".equals(componentData.getType()) && !logCreated) {
                        boolean hasData = false;
                        String senderType = "";
                        
                        // Check for File Sender (has fileName)
                        if (context.contains("fileName")) {
                            hasData = true;
                            senderType = "File";
                            String fileName = context.get("fileName", String.class);
                            Long fileSize = context.get("fileSize", Long.class);
                            log.info("📄 File Sender found file: {} ({} bytes)", fileName, fileSize);
                        }
                        // Check for JDBC Sender (has recordCount > 0)
                        else if (context.contains("recordCount")) {
                            Integer recordCount = context.get("recordCount", Integer.class);
                            if (recordCount != null && recordCount > 0) {
                                hasData = true;
                                senderType = "JDBC";
                                log.info("🗄️ JDBC Sender found {} records", recordCount);
                            }
                        }
                        
                        // Only create log if data was found
                        if (hasData) {
                            ExecutionLog executionLog = executionLogService.startExecution(scenarioId, executionId);
                            logId = executionLog != null ? executionLog.getId() : null;
                            logCreated = true;
                            
                            if (logId != null) {
                                executionLogService.updateComponentsExecuted(logId, components.size());
                            }
                            
                            log.info("📝 Execution log created for {} Sender", senderType);
                        }
                    }
                    
                    if ("Receiver".equals(componentData.getType()) && context.contains("outputFile")) {
                        String outputFile = context.get("outputFile", String.class);
                        log.info("📤 File delivered by Receiver: {}", outputFile);
                    }
                    
                } else {
                    log.error("❌ Component execution failed: {} - {}", 
                             componentData.getType(), componentData.getLabel());
                    
                    // Only mark as failed if log was created (data was found but processing failed)
                    if (logCreated) {
                        overallSuccess = false;
                        break;
                    } else {
                        // No data found, don't create log, just skip this iteration
                        log.debug("⏭️ Skipping execution log - no data found to process");
                        return;
                    }
                }
            }
            
            // ✅ Complete execution log only if it was created
            if (logCreated && logId != null) {
                executionLogService.completeExecution(logId, overallSuccess, context);
            }
            
            if (overallSuccess) {
                String successMessage = buildSuccessMessage(context);
                log.info("🎉 File transfer completed successfully: {}", successMessage);
            } else {
                log.error("💥 File transfer failed: {}", scenarioName);
            }
            
        } catch (Exception e) {
            log.error("❌ Error executing file transfer scenario {}: {}", scenarioName, e.getMessage(), e);
            // ✅ Complete execution log with error only if it was created
            if (logCreated && logId != null) {
                ExecutionContext errorContext = new ExecutionContext();
                errorContext.put("lastError", e.getMessage());
                executionLogService.completeExecution(logId, false, errorContext);
            }
        }
    }
    
    private String buildSuccessMessage(ExecutionContext context) {
        StringBuilder message = new StringBuilder("File transfer completed: ");
        
        if (context.contains("fileName") && context.contains("outputFile")) {
            String sourceFile = context.get("sourceFile", String.class);
            String outputFile = context.get("outputFile", String.class);
            Long fileSize = context.get("fileSize", Long.class);
            
            message.append(String.format("'%s' (%d bytes) → '%s'", 
                sourceFile, fileSize, outputFile));
        } else {
            message.append("Files transferred successfully");
        }
        
        return message.toString();
    }

    private JSONObject parseCanvasData(String canvasData) {
        try {
            if (canvasData == null || canvasData.trim().isEmpty()) {
                log.warn("⚠️ Canvas data is empty, using default structure");
                return new JSONObject("{\"flow\":[],\"components\":[]}");
            }
            return new JSONObject(canvasData);
        } catch (Exception e) {
            log.error("❌ Invalid canvas data format: {}, using default", e.getMessage());
            return new JSONObject("{\"flow\":[],\"components\":[]}");
        }
    }

    private List<ComponentExecutionData> extractExecutionComponents(JSONObject canvasData) {
        List<ComponentExecutionData> components = new ArrayList<>();
        
        if (canvasData.has("flow")) {
            JSONArray flowArray = canvasData.getJSONArray("flow");
            for (int i = 0; i < flowArray.length(); i++) {
                JSONObject flowComponent = flowArray.getJSONObject(i);
                ComponentExecutionData execData = createComponentExecutionData(flowComponent);
                if (execData != null && isExecutableComponent(execData.getType())) {
                    components.add(execData);
                }
            }
        }
        
        log.info("📦 Extracted {} executable components from flow", components.size());
        return components;
    }

    private ComponentExecutionData createComponentExecutionData(JSONObject componentJson) {
        try {
            String type = componentJson.getString("type");
            String label = componentJson.optString("label", "");
            
            JSONObject configData = new JSONObject();
            
            if (componentJson.has("config") && componentJson.getJSONObject("config").has("data")) {
                configData = componentJson.getJSONObject("config").getJSONObject("data");
            } else if (componentJson.has("form") && componentJson.getJSONObject("form").has("data")) {
                configData = componentJson.getJSONObject("form").getJSONObject("data");
            }
            
            log.debug("🔧 Component config data for {}: {}", type, configData.toString());
            
            return ComponentExecutionData.builder()
                    .type(type)
                    .label(label)
                    .configData(configData)
                    .rawComponentJson(componentJson)
                    .build();
                    
        } catch (Exception e) {
            log.warn("Failed to create execution data for component: {}", e.getMessage());
            return null;
        }
    }

    private boolean isExecutableComponent(String type) {
        return type.equals("Sender") || type.equals("Receiver");
    }

    private Integer extractPollingInterval(JSONObject canvasData) {
        try {
            List<ComponentExecutionData> components = extractExecutionComponents(canvasData);
            for (ComponentExecutionData component : components) {
                if ("Sender".equals(component.getType())) {
                    // ✅ PERBAIKAN: Ambil pollingInterval dari config Sender
                    if (component.getConfigData().has("pollingInterval")) {
                        int interval = component.getConfigData().getInt("pollingInterval");
                        log.info("🎯 Using polling interval from Sender config: {} seconds", interval);
                        return interval;
                    }
                }
            }
            log.warn("⚠️ No pollingInterval found in Sender config, using default: 60 seconds");
            return 60;
        } catch (Exception e) {
            log.warn("Error extracting polling interval, using default: {}", e.getMessage());
            return 60;
        }
    }

    @Transactional
    public void ensureRunStatusExists(Long fileId) {
        try {
            File file = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found"));
            
            String currentCanvasData = file.getCanvasData();
            if (currentCanvasData == null || !currentCanvasData.contains("runStatus")) {
                log.info("🔧 Adding runStatus to canvas_data for file: {}", file.getName());
                file.setRunStatus(0);
                fileRepository.save(file);
                log.info("✅ runStatus added to canvas_data");
            }
        } catch (Exception e) {
            log.error("❌ Error ensuring runStatus exists: {}", e.getMessage());
        }
    }

    public Map<String, Object> getScenarioDebugInfo(Long fileId, User currentUser) {
        try {
            File file = fileRepository.findById(fileId).orElse(null);
            Map<String, Object> debugInfo = new java.util.HashMap<>();
            
            if (file != null) {
                debugInfo.put("fileId", file.getId());
                debugInfo.put("fileName", file.getName());
                debugInfo.put("fileType", file.getSubfolderType());
                debugInfo.put("runStatus", file.getRunStatus());
                debugInfo.put("project", file.getProject() != null ? file.getProject().getName() : "null");
                debugInfo.put("projectId", file.getProject() != null ? file.getProject().getId() : "null");
                debugInfo.put("hasCanvasData", file.getCanvasData() != null && !file.getCanvasData().isEmpty());
                debugInfo.put("canvasDataContainsRunStatus", file.getCanvasData() != null && file.getCanvasData().contains("runStatus"));
                debugInfo.put("isRunningInMemory", runningScenarios.getOrDefault(fileId, false));
                debugInfo.put("hasScheduledTask", scheduledTasks.containsKey(fileId));
                
                // ✅ PERBAIKAN: Tambah info polling interval
                if (file.getCanvasData() != null) {
                    try {
                        JSONObject canvasData = new JSONObject(file.getCanvasData());
                        Integer pollingInterval = extractPollingInterval(canvasData);
                        debugInfo.put("pollingInterval", pollingInterval);
                    } catch (Exception e) {
                        debugInfo.put("pollingInterval", "error: " + e.getMessage());
                    }
                }
            }
            
            return debugInfo;
        } catch (Exception e) {
            log.error("Error getting debug info: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
}