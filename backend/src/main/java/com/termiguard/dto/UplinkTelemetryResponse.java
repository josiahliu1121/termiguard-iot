package com.termiguard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UplinkTelemetryResponse {

    private String status;
    private String message;
    private String eventId;
    private String stationId;
    private String serverTime;
    private StationConfigSync config;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StationConfigSync {
        private BigDecimal moistureThreshold;
        private Integer samplingFrequencyMinutes;
        private Integer heartbeatIntervalHours;
    }
}
