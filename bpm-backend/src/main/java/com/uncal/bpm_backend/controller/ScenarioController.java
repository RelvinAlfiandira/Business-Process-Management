package com.uncal.bpm_backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.uncal.bpm_backend.model.Scenario;
import com.uncal.bpm_backend.model.User;
import com.uncal.bpm_backend.service.ScenarioService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/scenarios")
@RequiredArgsConstructor
public class ScenarioController {

    private final ScenarioService scenarioService;

    @PostMapping
    public ResponseEntity<?> saveScenario(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal User currentUser) {
        try {
            System.out.println("🎯 SAVE SCENARIO - User: " + currentUser.getUsername());
            System.out.println("🎯 SAVE SCENARIO - Request: " + request);

            Scenario savedScenario = scenarioService.saveScenario(request, currentUser);
            
            return ResponseEntity.ok(Map.of(
                "message", "Scenario saved successfully",
                "version", savedScenario.getVersion(),
                "id", savedScenario.getId()
            ));
        } catch (Exception e) {
            System.err.println("❌ SAVE SCENARIO - Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
}