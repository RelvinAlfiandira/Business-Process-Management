package com.uncal.bpm_backend.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.uncal.bpm_backend.dto.ExecutionLogResponse;
import com.uncal.bpm_backend.model.ExecutionLog;
import com.uncal.bpm_backend.model.User;
import com.uncal.bpm_backend.service.ExecutionLogService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/execution-logs")
@RequiredArgsConstructor
@Slf4j
public class ExecutionLogController {
    
    private final ExecutionLogService executionLogService;
    
    /**
     * Get execution history for a specific scenario file
     */
    @GetMapping("/scenario/{fileId}")
    public ResponseEntity<List<ExecutionLogResponse>> getScenarioHistory(
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "50") int limit,
            @AuthenticationPrincipal User currentUser) {
        
        log.info("📊 Getting execution history for file: {}, limit: {}", fileId, limit);
        
        List<ExecutionLog> logs = executionLogService.getExecutionHistory(fileId, limit);
        List<ExecutionLogResponse> response = logs.stream()
                .map(ExecutionLogResponse::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all execution history for a specific scenario file (no limit)
     */
    @GetMapping("/scenario/{fileId}/all")
    public ResponseEntity<List<ExecutionLogResponse>> getAllScenarioHistory(
            @PathVariable Long fileId,
            @AuthenticationPrincipal User currentUser) {
        
        log.info("📊 Getting ALL execution history for file: {}", fileId);
        
        List<ExecutionLog> logs = executionLogService.getAllExecutionHistory(fileId);
        List<ExecutionLogResponse> response = logs.stream()
                .map(ExecutionLogResponse::fromEntity)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get statistics for a specific scenario file
     */
    @GetMapping("/scenario/{fileId}/stats")
    public ResponseEntity<Map<String, Object>> getScenarioStats(
            @PathVariable Long fileId,
            @AuthenticationPrincipal User currentUser) {
        
        log.info("📈 Getting execution stats for file: {}", fileId);
        
        Map<String, Object> stats = executionLogService.getScenarioStats(fileId);
        return ResponseEntity.ok(stats);
    }
}
