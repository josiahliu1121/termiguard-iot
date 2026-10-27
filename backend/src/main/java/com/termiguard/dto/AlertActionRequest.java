package com.termiguard.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertActionRequest {
    private String action; // 'verify' | 'treat' | 'resolve' | 'mark_false_alarm'
    private String notes;
    private String technician;
    private String toxicantBatch;
}
