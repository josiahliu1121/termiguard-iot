package com.termiguard.controller;

import com.termiguard.dto.UplinkTelemetryRequest;
import com.termiguard.dto.UplinkTelemetryResponse;
import com.termiguard.service.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/telemetry")
@CrossOrigin(origins = "*")
public class TelemetryController {

    private final EventService eventService;

    public TelemetryController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * Cellular 4G Uplink Ingestion Endpoint
     * Receives 15-min acoustic sampling cycles or 24-hr keepalive heartbeats from ESP32 nodes.
     */
    @PostMapping("/uplink")
    public ResponseEntity<UplinkTelemetryResponse> receiveUplink(
            @RequestBody UplinkTelemetryRequest request,
            @RequestHeader(value = "X-Node-API-Key", required = false) String apiKey) {
        log.info("Received 4G telemetry uplink packet from station: {}, type: {}, apiKeyPresent: {}",
                request != null ? request.getStationId() : "NULL",
                request != null ? request.getEventType() : "NULL",
                apiKey != null && !apiKey.isEmpty());

        UplinkTelemetryResponse response = eventService.processUplink(request);
        return ResponseEntity.ok(response);
    }
}
