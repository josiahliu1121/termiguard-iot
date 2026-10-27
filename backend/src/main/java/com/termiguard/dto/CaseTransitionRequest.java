package com.termiguard.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseTransitionRequest {
    private String nextStatus;
    private String technician;
    private String notes;
    private String toxicantBatch;
}
