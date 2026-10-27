package com.termiguard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StationCompositeDto {
    // Station Base Information
    private String id;
    private String code;
    private String name;
    private String description;
    private String type;
    private Double[] coordinates; // [lat, lng]
    private String plantationZone;
    private String treeTag;
    private Boolean isProfileComplete;
    private Double moistureThreshold;
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

    // Composite & Derived Properties for Frontend
    private String zone;
    private String status; // 'online' | 'warning' | 'offline'
    private String confidenceTier;
    private String operatingMode; // 'dormant_heartbeat' | 'acoustic_triggered'
    private Boolean isAcousticGated;
    private Double soilMoisture;
    private Integer batteryLevel;
    private Double batteryVoltage;
    private Double temperature;
    private Double humidity;
    private Integer activityScore;
    private Integer aiRiskScore;
    private Integer rssi;
    private Double snr;
    private String lastSeen;
    private String lastHeartbeatTime;
    private String lastAcousticCaptureTime;
    private String nextExpectedTransmission;
    private TelemetryEventDto latestEvent;
    private List<Double> mfccFeatures;
}
