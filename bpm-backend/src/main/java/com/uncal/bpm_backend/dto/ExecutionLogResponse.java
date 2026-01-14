package com.uncal.bpm_backend.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionLogResponse {
    private Long id;
    private String executionId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private Integer componentsExecuted;
    private String sourceFile;
    private String destinationFile;
    private Integer recordsProcessed;
    private String errorMessage;
    private String scenarioFileName;
    
    public static ExecutionLogResponse fromEntity(com.uncal.bpm_backend.model.ExecutionLog log) {
        ExecutionLogResponse response = new ExecutionLogResponse();
        response.setId(log.getId());
        response.setExecutionId(log.getExecutionId());
        response.setStatus(log.getStatus());
        response.setStartTime(log.getStartTime());
        response.setEndTime(log.getEndTime());
        response.setDurationMs(log.getDurationMs());
        response.setComponentsExecuted(log.getComponentsExecuted());
        response.setSourceFile(log.getSourceFile());
        response.setDestinationFile(log.getDestinationFile());
        response.setRecordsProcessed(log.getRecordsProcessed());
        response.setErrorMessage(log.getErrorMessage());
        
        // Handle lazy loading issue - wrap in try-catch
        try {
            response.setScenarioFileName(log.getScenarioFile() != null ? log.getScenarioFile().getName() : null);
        } catch (Exception e) {
            response.setScenarioFileName(null);
        }
        
        return response;
    }
}
