package com.uncal.bpm_backend.repository;

import com.uncal.bpm_backend.model.ExecutionLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long> {
    
    @Query("SELECT e FROM ExecutionLog e JOIN FETCH e.scenarioFile WHERE e.scenarioFile.id = ?1 ORDER BY e.startTime DESC")
    List<ExecutionLog> findByScenarioFileIdOrderByStartTimeDesc(Long scenarioFileId, Pageable pageable);
    
    @Query("SELECT e FROM ExecutionLog e JOIN FETCH e.scenarioFile WHERE e.scenarioFile.id = ?1 ORDER BY e.startTime DESC")
    List<ExecutionLog> findByScenarioFileIdOrderByStartTimeDesc(Long scenarioFileId);
    
    long countByScenarioFileId(Long scenarioFileId);
    
    @Query("SELECT COUNT(e) FROM ExecutionLog e WHERE e.scenarioFile.id = ?1 AND e.status = 'SUCCESS'")
    long countSuccessByScenarioFileId(Long scenarioFileId);
    
    @Query("SELECT e FROM ExecutionLog e JOIN FETCH e.scenarioFile WHERE e.scenarioFile.id = ?1 ORDER BY e.startTime DESC")
    List<ExecutionLog> findTop10ByScenarioFileId(Long scenarioFileId, Pageable pageable);
}
