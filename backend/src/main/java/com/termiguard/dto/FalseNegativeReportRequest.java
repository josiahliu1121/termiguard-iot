package com.termiguard.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FalseNegativeReportRequest {
    private String notes;
    private String technician;
    private String probableCause;
}
