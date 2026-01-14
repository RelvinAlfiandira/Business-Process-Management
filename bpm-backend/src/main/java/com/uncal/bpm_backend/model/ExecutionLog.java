package com.uncal.bpm_backend.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "execution_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_file_id", nullable = false)
    private File scenarioFile;

    @Column(name = "execution_id", nullable = false, length = 100)
    private String executionId;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // 'SUCCESS', 'FAILED', 'RUNNING'

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "components_executed")
    private Integer componentsExecuted;

    @Column(name = "source_file", length = 500)
    private String sourceFile;

    @Column(name = "destination_file", length = 500)
    private String destinationFile;

    @Column(name = "records_processed")
    private Integer recordsProcessed;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
