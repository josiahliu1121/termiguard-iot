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
public class TelemetryEventDto {
    private String id;
    private String stationId;
    private String timestamp;
    private String eventType;
    private Integer batteryLevel;
    private Double batteryVoltage;
    private Double soilMoisture;
    private Integer acousticScore;
    private Double temperature;
    private Double humidity;
    private Integer rssi;
    private Double snr;
    private String predictLevel;
    private Integer aiRiskScore;
    private Boolean isMoistureTriggered;
    private List<Double> mfccFeatures;
    private String validationOutcome;
    private String validatedAt;
    private String validatedBy;
    private String validationNotes;
}
