package com.termiguard.controller;

import com.termiguard.dto.TelemetryEventDto;
import com.termiguard.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@CrossOrigin(origins = "*")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<List<TelemetryEventDto>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllEvents());
    }
}
