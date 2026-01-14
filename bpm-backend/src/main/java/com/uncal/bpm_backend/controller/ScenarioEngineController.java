package com.uncal.bpm_backend.controller;

import com.uncal.bpm_backend.model.User;
import com.uncal.bpm_backend.service.ScenarioEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/scenario")
@RequiredArgsConstructor
public class ScenarioEngineController {

    private final ScenarioEngineService scenarioEngineService;

    @PostMapping("/{fileId}/run")
    public ResponseEntity<?> runScenario(
            @PathVariable Long fileId,
            @AuthenticationPrincipal User currentUser) {
        try {
            scenarioEngineService.runScenario(fileId, currentUser);
            return ResponseEntity.ok().body(Map.of("message", "Scenario started successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{fileId}/stop")
    public ResponseEntity<?> stopScenario(
            @PathVariable Long fileId,
            @AuthenticationPrincipal User currentUser) {
        try {
            scenarioEngineService.stopScenario(fileId, currentUser);
            return ResponseEntity.ok().body(Map.of("message", "Scenario stopped successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{fileId}/status")
    public ResponseEntity<?> getScenarioStatus(
            @PathVariable Long fileId,
            @AuthenticationPrincipal User currentUser) {
        try {
            Integer status = scenarioEngineService.getScenarioStatus(fileId, currentUser);
            return ResponseEntity.ok().body(Map.of("runStatus", status));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}