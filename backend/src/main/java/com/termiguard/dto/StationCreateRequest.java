package com.termiguard.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationCreateRequest {
    private String id;
    private String code;
    private String name;
    private String description;
    private String type;
    private List<Double> coordinates;
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
}
