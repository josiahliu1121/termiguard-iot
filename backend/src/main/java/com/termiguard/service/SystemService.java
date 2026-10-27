package com.termiguard.service;

import com.termiguard.dto.GroundTruthMetricsDto;
import com.termiguard.dto.SystemStatusDto;

public interface SystemService {
    SystemStatusDto getSystemStatus();
    GroundTruthMetricsDto getGroundTruthMetrics();
}
