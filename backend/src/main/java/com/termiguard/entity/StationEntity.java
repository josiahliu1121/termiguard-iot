package com.termiguard.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationEntity {
    private String id;
    private String code;
    private String name;
    private String description;
    private String type;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String plantationZone;
    private String treeTag;
    private Boolean isProfileComplete;
    private BigDecimal moistureThreshold;
    private Integer samplingFrequencyMinutes;
    private Integer heartbeatIntervalHours;
    private Boolean isSolarPowered;
    private String installedAt;
    private Integer threatIndex;
    private String threatLevel;
    private Integer windowHits;
    private Integer windowTotal;
    private String windowSummary;
    private String lastEvaluatedAt;
    private String humanVerificationStatus;
    private String lastVerifiedAt;
    private String verifiedCleanAt;
    private String verifiedBy;
    private String technicianNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
