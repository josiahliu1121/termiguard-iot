package com.termiguard.controller;

import com.termiguard.dto.AlertActionRequest;
import com.termiguard.entity.AlertEntity;
import com.termiguard.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@CrossOrigin(origins = "*")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<List<AlertEntity>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertEntity> getAlertById(@PathVariable String id) {
        AlertEntity alert = alertService.getAlertById(id);
        if (alert == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(alert);
    }

    @PostMapping("/{id}/action")
    public ResponseEntity<AlertEntity> performAction(
            @PathVariable String id,
            @RequestBody AlertActionRequest request) {
        return ResponseEntity.ok(alertService.performAction(id, request));
    }
}
