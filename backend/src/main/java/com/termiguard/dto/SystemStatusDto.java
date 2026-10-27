package com.termiguard.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusDto {
    private Boolean gatewayOnline;
    private String gatewayBackhaul;
    private Boolean masterConnected;
    private Integer activeStationsCount;
    private Integer totalStationsCount;
    private Boolean aiInferenceServiceOnline;
    private String lastPacketTime;
    private Boolean timescaleDbHealthy;
}
