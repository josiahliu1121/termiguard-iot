package com.termiguard.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaseCreateRequest {
    private String stationId;
    private String finding; // 'termites_present' | 'clean_no_termites'
    private String technician;
    private String notes;
    private String probableCause;
}
