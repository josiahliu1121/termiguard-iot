package com.termiguard.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UplinkTelemetryRequest {

    @JsonAlias({"stationId", "station_id"})
    private String stationId;

    @JsonAlias({"eventType", "event_type"})
    private String eventType;

    @JsonAlias({"batteryLevel", "battery_level"})
    private Integer batteryLevel;

    @JsonAlias({"batteryVoltage", "battery_voltage"})
    private BigDecimal batteryVoltage;

    @JsonAlias({"soilMoisture", "soil_moisture"})
    private BigDecimal soilMoisture;

    @JsonAlias({"acousticScore", "acoustic_score"})
    private Integer acousticScore;

    private BigDecimal temperature;

    private BigDecimal humidity;

    private Integer rssi;

    private BigDecimal snr;

    @JsonAlias({"isMoistureTriggered", "is_moisture_triggered"})
    private Boolean isMoistureTriggered;

    @JsonAlias({"mfccFeatures", "mfcc_features"})
    private List<BigDecimal> mfccFeatures;

    private String timestamp;
}
