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
public class AlertEntity {
    private String id;
    private String stationId;
    private String stationName;
    private String code;
    private String timestamp;
    private String confidenceTier;
    private String status;
    private String sopStage;
    private String summary;
    private Integer activityScore;
    private String technicianNotes;
    private String verificationDate;
    private String treatmentDate;
    private String toxicantBatch;
    private String resolutionDate;
    private String validationOutcome;
    private String probableCause;
    private String verifiedBy;
    private LocalDateTime createdAt;
}
