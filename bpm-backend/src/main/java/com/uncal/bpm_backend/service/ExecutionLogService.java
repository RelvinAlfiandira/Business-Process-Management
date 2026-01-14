package com.uncal.bpm_backend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uncal.bpm_backend.model.ExecutionLog;
import com.uncal.bpm_backend.model.File;
import com.uncal.bpm_backend.repository.ExecutionLogRepository;
import com.uncal.bpm_backend.repository.FileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionLogService {
    
    private final ExecutionLogRepository executionLogRepository;
    private final FileRepository fileRepository;
    
    @Transactional
    public ExecutionLog startExecution(Long scenarioFileId, String executionId) {
        try {
            File scenarioFile = fileRepository.findById(scenarioFileId)
                    .orElseThrow(() -> new RuntimeException("Scenario file not found"));
            
            ExecutionLog executionLog = new ExecutionLog();
            executionLog.setScenarioFile(scenarioFile);
            executionLog.setExecutionId(executionId);
            executionLog.setStatus("RUNNING");
            executionLog.setStartTime(LocalDateTime.now());
            executionLog.setComponentsExecuted(0);
            
            ExecutionLog saved = executionLogRepository.save(executionLog);
            // Use class-level logger instead of local variable
            return saved;
        } catch (Exception e) {
            log.error("❌ Error starting execution log: {}", e.getMessage());
            return null;
        }
    }
    
    @Transactional
    public void completeExecution(Long logId, boolean success, ExecutionContext context) {
        try {
            ExecutionLog executionLog = executionLogRepository.findById(logId).orElse(null);
            if (executionLog == null) {
                log.warn("⚠️ Execution log not found: {}", logId);
                return;
            }
            
            executionLog.setStatus(success ? "SUCCESS" : "FAILED");
            executionLog.setEndTime(LocalDateTime.now());
            
            // Calculate duration
            if (executionLog.getStartTime() != null && executionLog.getEndTime() != null) {
                long duration = java.time.Duration.between(executionLog.getStartTime(), executionLog.getEndTime()).toMillis();
                executionLog.setDurationMs(duration);
            }
            
            // Extract context data
            if (context != null) {
                // For File-based scenarios
                if (context.contains("sourceFile")) {
                    executionLog.setSourceFile(context.get("sourceFile", String.class));
                }
                if (context.contains("outputFile")) {
                    executionLog.setDestinationFile(context.get("outputFile", String.class));
                }
                
                // For JDBC-based scenarios
                if (context.contains("sourceTable")) {
                    String sourceTable = context.get("sourceTable", String.class);
                    String dbType = context.get("dbType", String.class);
                    executionLog.setSourceFile(dbType + ":" + sourceTable);
                }
                if (context.contains("destinationTable")) {
                    String destTable = context.get("destinationTable", String.class);
                    String destDbType = context.contains("destinationDbType") ? 
                                        context.get("destinationDbType", String.class) : 
                                        context.get("dbType", String.class);
                    executionLog.setDestinationFile(destDbType + ":" + destTable);
                }
                
                // Records processed
                if (context.contains("recordsProcessed")) {
                    executionLog.setRecordsProcessed(context.get("recordsProcessed", Integer.class));
                } else if (context.contains("recordCount")) {
                    executionLog.setRecordsProcessed(context.get("recordCount", Integer.class));
                }
                
                // Error message
                if (context.contains("lastError")) {
                    executionLog.setErrorMessage(context.get("lastError", String.class));
                }
            }
            
            executionLogRepository.save(executionLog);
            log.info("✅ Completed execution log: {} with status: {}", executionLog.getExecutionId(), executionLog.getStatus());
        } catch (Exception e) {
            log.error("❌ Error completing execution log: {}", e.getMessage());
        }
    }
    
    @Transactional
    public void updateComponentsExecuted(Long logId, int count) {
        try {
            ExecutionLog executionLog = executionLogRepository.findById(logId).orElse(null);
            if (executionLog != null) {
                executionLog.setComponentsExecuted(count);
                executionLogRepository.save(executionLog);
            }
        } catch (Exception e) {
            log.error("❌ Error updating components executed: {}", e.getMessage());
        }
    }
    
    public List<ExecutionLog> getExecutionHistory(Long scenarioFileId, int limit) {
        try {
            return executionLogRepository.findByScenarioFileIdOrderByStartTimeDesc(
                    scenarioFileId, PageRequest.of(0, limit));
        } catch (Exception e) {
            log.error("❌ Error getting execution history: {}", e.getMessage());
            return List.of();
        }
    }
    
    public List<ExecutionLog> getAllExecutionHistory(Long scenarioFileId) {
        try {
            return executionLogRepository.findByScenarioFileIdOrderByStartTimeDesc(scenarioFileId);
        } catch (Exception e) {
            log.error("❌ Error getting all execution history: {}", e.getMessage());
            return List.of();
        }
    }
    
    public Map<String, Object> getScenarioStats(Long scenarioFileId) {
        try {
            long totalExecutions = executionLogRepository.countByScenarioFileId(scenarioFileId);
            long successCount = executionLogRepository.countSuccessByScenarioFileId(scenarioFileId);
            
            double successRate = totalExecutions > 0 ? (successCount * 100.0 / totalExecutions) : 0;
            
            List<ExecutionLog> recentLogs = executionLogRepository.findTop10ByScenarioFileId(
                    scenarioFileId, PageRequest.of(0, 1));
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalExecutions", totalExecutions);
            stats.put("successCount", successCount);
            stats.put("failedCount", totalExecutions - successCount);
            stats.put("successRate", Math.round(successRate * 100.0) / 100.0);
            stats.put("lastExecution", recentLogs.isEmpty() ? null : recentLogs.get(0).getStartTime());
            
            return stats;
        } catch (Exception e) {
            log.error("❌ Error getting scenario stats: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
}