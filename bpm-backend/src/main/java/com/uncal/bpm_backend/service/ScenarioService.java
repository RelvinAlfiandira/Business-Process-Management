package com.uncal.bpm_backend.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uncal.bpm_backend.model.Scenario;
import com.uncal.bpm_backend.model.User;
import com.uncal.bpm_backend.repository.ScenarioRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;

    @Transactional
    public Scenario saveScenario(Map<String, Object> request, User currentUser) {
        try {
            System.out.println("💾 Saving scenario for user: " + currentUser.getUsername());
            
            // Extract data from request
            Object jsonData = request.get("jsonData");
            String project = (String) request.get("project");
            String scenarios = (String) request.get("scenarios");
            Integer runStatus = (Integer) request.get("run_status");
            Map<String, Object> metadata = (Map<String, Object>) request.get("metadata");

            // Create new scenario
            Scenario scenario = new Scenario();
            scenario.setJsonData(jsonData);
            scenario.setProject(project);
            scenario.setScenarios(scenarios);
            scenario.setRunStatus(runStatus != null ? runStatus : 0);
            scenario.setMetadata(metadata);
            scenario.setVersion(1);
            scenario.setCreatedBy(currentUser);
            scenario.setCreatedAt(LocalDateTime.now());

            Scenario savedScenario = scenarioRepository.save(scenario);
            
            System.out.println("✅ Scenario saved successfully - ID: " + savedScenario.getId());
            return savedScenario;
            
        } catch (Exception e) {
            log.error("Error saving scenario: ", e);
            throw new RuntimeException("Failed to save scenario: " + e.getMessage());
        }
    }
}