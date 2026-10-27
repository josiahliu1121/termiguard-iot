package com.termiguard.controller;

import com.termiguard.dto.GroundTruthMetricsDto;
import com.termiguard.dto.SystemStatusDto;
import com.termiguard.service.SystemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class SystemController {

    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    @GetMapping("/system/status")
    public ResponseEntity<SystemStatusDto> getSystemStatus() {
        return ResponseEntity.ok(systemService.getSystemStatus());
    }

    @GetMapping("/analytics/ground-truth-metrics")
    public ResponseEntity<GroundTruthMetricsDto> getGroundTruthMetrics() {
        return ResponseEntity.ok(systemService.getGroundTruthMetrics());
    }
}
