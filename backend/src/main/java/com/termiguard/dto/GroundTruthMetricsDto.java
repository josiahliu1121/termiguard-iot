package com.termiguard.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroundTruthMetricsDto {
    private Integer tp;
    private Integer fp;
    private Integer fn;
    private Integer tn;
    private Integer total;
    private Double precision;
    private Double recall;
    private Double accuracy;
    private Double falseAlarmRate;
    private Double missRate;
}
