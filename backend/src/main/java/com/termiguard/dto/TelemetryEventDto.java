package com.termiguard.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @JsonProperty("validationOutcome")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String validationOutcome;

    @JsonProperty("validatedAt")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String validatedAt;

    @JsonProperty("validatedBy")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String validatedBy;

    @JsonProperty("validationNotes")
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private String validationNotes;
}
