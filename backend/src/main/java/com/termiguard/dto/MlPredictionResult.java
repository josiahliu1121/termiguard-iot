package com.termiguard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlPredictionResult {
    private String predictLevel;
    private Integer aiRiskScore;
    private Double confidence;
    private Map<String, Double> probabilities;
    private Boolean fallbackUsed;
}
