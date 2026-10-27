package com.termiguard.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCaseEntity {
    private String id;
    private String stationId;
    private String stationCode;
    private String stationName;
    private String createdAt;
    private String status;
    private String technician;
    private String finding;
    private String probableCause;
    private String initialNotes;
    private String verifiedAt;
    private String treatmentStartedAt;
    private String toxicantBatch;
    private String treatmentNotes;
    private String monitoredAt;
    private String monitoringNotes;
    private String clearedAt;
    private String clearedNotes;
    private String terminatedAt;
    private String terminatedNotes;
    private String validationOutcome;
    private LocalDateTime updatedAt;
}
