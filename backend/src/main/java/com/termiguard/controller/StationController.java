package com.termiguard.controller;

import com.termiguard.dto.*;
import com.termiguard.service.EventService;
import com.termiguard.service.StationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stations")
@CrossOrigin(origins = "*")
public class StationController {

    private final StationService stationService;
    private final EventService eventService;

    public StationController(StationService stationService, EventService eventService) {
        this.stationService = stationService;
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<StationCompositeDto>> getAllStations() {
        return ResponseEntity.ok(stationService.getAllStations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StationCompositeDto> getStationById(@PathVariable String id) {
        StationCompositeDto station = stationService.getStationById(id);
        if (station == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(station);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StationCompositeDto> modifyStation(
            @PathVariable String id,
            @RequestBody StationModifyRequest request) {
        return ResponseEntity.ok(stationService.modifyStation(id, request));
    }

    @PostMapping("/{id}/report-false-negative")
    public ResponseEntity<Map<String, Object>> reportFalseNegative(
            @PathVariable String id,
            @RequestBody(required = false) FalseNegativeReportRequest request) {
        return ResponseEntity.ok(stationService.reportFalseNegative(id, request));
    }

    @PostMapping("/{id}/routine-inspection")
    public ResponseEntity<Map<String, Object>> logRoutineInspection(
            @PathVariable String id,
            @RequestBody(required = false) RoutineInspectionRequest request) {
        return ResponseEntity.ok(stationService.logRoutineInspection(id, request));
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<List<TelemetryEventDto>> getStationEvents(@PathVariable String id) {
        return ResponseEntity.ok(eventService.getEventsByStation(id));
    }
}
