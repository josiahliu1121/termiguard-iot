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
public class GatewayInfoEntity {
    private String id;
    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String backhaul;
    private Integer signalDbm;
    private String ipAddress;
    private Boolean isOnline;
    private LocalDateTime updatedAt;
}
