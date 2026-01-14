package com.uncal.bpm_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.uncal.bpm_backend.model.Scenario;

@Repository
public interface ScenarioRepository extends JpaRepository<Scenario, Long> {
    // Custom queries can be added here if needed
}