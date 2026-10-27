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
public class TelemetryEventEntity {
    private String id;
    private String stationId;
    private String timestamp;
    private String eventType;
    private Integer batteryLevel;
    private BigDecimal batteryVoltage;
    private BigDecimal soilMoisture;
    private Integer acousticScore;
    private BigDecimal temperature;
    private BigDecimal humidity;
    private Integer rssi;
    private BigDecimal snr;
    private String predictLevel;
    private Integer aiRiskScore;
    private Boolean isMoistureTriggered;
    private String mfccFeatures;
    private String validationOutcome;
    private String validatedAt;
    private String validatedBy;
    private String validationNotes;
    private LocalDateTime createdAt;
}
