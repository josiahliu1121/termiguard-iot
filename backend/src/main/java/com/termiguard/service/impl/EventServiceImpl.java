package com.termiguard.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.termiguard.dto.StationCompositeDto;
import com.termiguard.dto.TelemetryEventDto;
import com.termiguard.entity.AlertEntity;
import com.termiguard.entity.StationEntity;
import com.termiguard.entity.TelemetryEventEntity;
import com.termiguard.mapper.AlertMapper;
import com.termiguard.mapper.StationMapper;
import com.termiguard.mapper.TelemetryEventMapper;
import com.termiguard.service.EventService;
import com.termiguard.service.StationService;
import com.termiguard.websocket.TelemetryWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EventServiceImpl implements EventService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TelemetryEventMapper eventMapper;
    private final StationMapper stationMapper;
    private final AlertMapper alertMapper;
    private final StationService stationService;
    private final TelemetryWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    public EventServiceImpl(TelemetryEventMapper eventMapper,
                            StationMapper stationMapper,
                            AlertMapper alertMapper,
                            @Lazy StationService stationService,
                            TelemetryWebSocketHandler webSocketHandler,
                            ObjectMapper objectMapper) {
        this.eventMapper = eventMapper;
        this.stationMapper = stationMapper;
        this.alertMapper = alertMapper;
        this.stationService = stationService;
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<TelemetryEventDto> getAllEvents() {
        return eventMapper.selectAll().stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<TelemetryEventDto> getEventsByStation(String stationId) {
        return eventMapper.selectByStationId(stationId).stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public TelemetryEventDto getLatestEventForStation(String stationId) {
        TelemetryEventEntity entity = eventMapper.selectLatestByStationId(stationId);
        return entity != null ? entityToDto(entity) : null;
    }

    @Override
    @Transactional
    public TelemetryEventDto insertEvent(TelemetryEventEntity entity) {
        if (entity.getId() == null || entity.getId().trim().isEmpty()) {
            entity.setId("evt-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000));
        }
        if (entity.getTimestamp() == null || entity.getTimestamp().trim().isEmpty()) {
            entity.setTimestamp(LocalDateTime.now().format(DATE_FORMATTER));
        }
        if (entity.getEventType() == null) {
            entity.setEventType("daily_heartbeat");
        }
        if (entity.getBatteryLevel() == null) {
            entity.setBatteryLevel(95);
        }
        if (entity.getBatteryVoltage() == null) {
            entity.setBatteryVoltage(BigDecimal.valueOf(4.00));
        }
        if (entity.getSoilMoisture() == null) {
            entity.setSoilMoisture(BigDecimal.valueOf(55.00));
        }
        if (entity.getTemperature() == null) {
            entity.setTemperature(BigDecimal.valueOf(29.5));
        }
        if (entity.getHumidity() == null) {
            entity.setHumidity(BigDecimal.valueOf(78.0));
        }
        if (entity.getRssi() == null) {
            entity.setRssi(-80);
        }
        if (entity.getSnr() == null) {
            entity.setSnr(BigDecimal.valueOf(8.0));
        }
        if (entity.getPredictLevel() == null) {
            entity.setPredictLevel("Safe");
        }
        if (entity.getAiRiskScore() == null) {
            entity.setAiRiskScore(10);
        }
        if (entity.getIsMoistureTriggered() == null) {
            entity.setIsMoistureTriggered(false);
        }

        eventMapper.insert(entity);
        TelemetryEventDto dto = entityToDto(entity);

        // Broadcast event_inserted
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("type", "event_inserted");
        wsPayload.put("event", dto);
        webSocketHandler.broadcast(wsPayload);

        // Evaluate temporal persistence & update station threat metrics
        evaluateTemporalPersistence(entity.getStationId());

        return dto;
    }

    @Override
    @Transactional
    public StationCompositeDto evaluateTemporalPersistence(String stationId) {
        StationEntity station = stationMapper.selectById(stationId);
        if (station == null) {
            return null;
        }

        List<TelemetryEventEntity> stationEvents = eventMapper.selectByStationId(stationId);
        if (stationEvents.isEmpty()) {
            return stationService.composeStation(station, null);
        }

        // Take the latest up to 6 events (N=6 sliding window)
        int windowSize = 6;
        int startIndex = Math.max(0, stationEvents.size() - windowSize);
        List<TelemetryEventEntity> windowEvents = stationEvents.subList(startIndex, stationEvents.size());

        int windowHits = 0;
        int totalEvaluated = windowEvents.size();
        int sumRisk = 0;

        for (TelemetryEventEntity ev : windowEvents) {
            boolean isTrigger = (ev.getAcousticScore() != null && ev.getAcousticScore() >= 60)
                    || (Boolean.TRUE.equals(ev.getIsMoistureTriggered())
                    && ("High".equalsIgnoreCase(ev.getPredictLevel()) || "Medium".equalsIgnoreCase(ev.getPredictLevel())));
            if (isTrigger) {
                windowHits++;
            }
            sumRisk += (ev.getAiRiskScore() != null ? ev.getAiRiskScore() : 10);
        }

        int avgRisk = totalEvaluated > 0 ? (sumRisk / totalEvaluated) : 10;
        int threatIndex = (int) Math.round((windowHits / (double) windowSize) * 70.0 + (avgRisk * 0.3));
        threatIndex = Math.max(5, Math.min(100, threatIndex));

        String threatLevel;
        if (threatIndex >= 75) {
            threatLevel = "High";
        } else if (threatIndex >= 50) {
            threatLevel = "Medium";
        } else if (threatIndex >= 25) {
            threatLevel = "Low";
        } else {
            threatLevel = "Safe";
        }

        String summary = windowHits + "/" + windowSize + " active detections in past 90 mins";
        if (windowHits == 0) {
            summary = "0/6 triggers - Gated Asleep (Normal Baseline)";
        }

        String now = LocalDateTime.now().format(DATE_FORMATTER);

        station.setThreatIndex(threatIndex);
        station.setThreatLevel(threatLevel);
        station.setWindowHits(windowHits);
        station.setWindowTotal(windowSize);
        station.setWindowSummary(summary);
        station.setLastEvaluatedAt(now);

        stationMapper.update(station);

        // Check if High threat and trigger automated alert if none exists
        if ("High".equalsIgnoreCase(threatLevel)) {
            AlertEntity existing = alertMapper.selectActiveByStationId(stationId);
            if (existing == null) {
                String alertId = "ALT-" + String.format("%04d", System.currentTimeMillis() % 10000);
                AlertEntity newAlert = AlertEntity.builder()
                        .id(alertId)
                        .stationId(stationId)
                        .stationName(station.getName())
                        .code(station.getCode())
                        .timestamp(now)
                        .confidenceTier(threatLevel)
                        .status("unverified")
                        .sopStage("verify")
                        .summary("Persistent acoustic foraging clusters detected (" + summary + "). Scheduled verification triggered.")
                        .activityScore(threatIndex)
                        .technicianNotes("Automated alert generated from multi-event temporal persistence engine.")
                        .build();
                alertMapper.insert(newAlert);

                Map<String, Object> alertWs = new HashMap<>();
                alertWs.put("type", "alert_triggered");
                alertWs.put("alert", newAlert);
                webSocketHandler.broadcast(alertWs);
            }
        }

        TelemetryEventDto latest = entityToDto(stationEvents.get(stationEvents.size() - 1));
        StationCompositeDto composed = stationService.composeStation(station, latest);

        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("type", "station_updated");
        wsPayload.put("station", composed);
        webSocketHandler.broadcast(wsPayload);

        return composed;
    }

    @Override
    @Transactional
    public Map<String, Object> applyGroundTruthValidation(String stationId, String finding, String technician, String notes) {
        StationEntity station = stationMapper.selectById(stationId);
        if (station == null) {
            throw new RuntimeException("Station " + stationId + " not found");
        }

        LocalDateTime now = LocalDateTime.now();
        String nowIso = now.format(DATE_FORMATTER);

        LocalDateTime sevenDaysAgo = now.minusDays(7);
        String cutoffTime = sevenDaysAgo.format(DATE_FORMATTER);

        if (station.getLastVerifiedAt() != null && !station.getLastVerifiedAt().isEmpty()) {
            if (station.getLastVerifiedAt().compareTo(cutoffTime) > 0) {
                cutoffTime = station.getLastVerifiedAt();
            }
        }

        List<TelemetryEventEntity> candidates = eventMapper.selectEventsForValidation(stationId, cutoffTime, nowIso);

        int tp = 0;
        int fp = 0;
        int fn = 0;
        int tn = 0;
        int labeledCount = 0;

        for (TelemetryEventEntity ev : candidates) {
            boolean isHighOrMed = "High".equalsIgnoreCase(ev.getPredictLevel()) || "Medium".equalsIgnoreCase(ev.getPredictLevel());
            String outcome;
            if ("termites_present".equalsIgnoreCase(finding)) {
                if (isHighOrMed) {
                    outcome = "TP";
                    tp++;
                } else {
                    outcome = "FN";
                    fn++;
                }
            } else {
                if (isHighOrMed) {
                    outcome = "FP";
                    fp++;
                } else {
                    outcome = "TN";
                    tn++;
                }
            }

            eventMapper.updateValidationOutcome(ev.getId(), outcome, nowIso, technician, notes);
            labeledCount++;
        }

        // Update station record
        String newStatus;
        if ("termites_present".equalsIgnoreCase(finding)) {
            newStatus = "field_verified_infestation";
        } else {
            boolean isSuspicious = "High".equalsIgnoreCase(station.getThreatLevel()) || "Medium".equalsIgnoreCase(station.getThreatLevel());
            newStatus = isSuspicious ? "false_alarm" : "verified_clean";
        }

        station.setHumanVerificationStatus(newStatus);
        if ("clean_no_termites".equalsIgnoreCase(finding)) {
            station.setVerifiedCleanAt(nowIso);
        }
        station.setLastVerifiedAt(nowIso);
        station.setVerifiedBy(technician);
        station.setTechnicianNotes(notes);

        stationMapper.update(station);

        TelemetryEventDto latest = getLatestEventForStation(stationId);
        StationCompositeDto composed = stationService.composeStation(station, latest);

        Map<String, Object> result = new HashMap<>();
        result.put("labeledCount", labeledCount);
        result.put("tp", tp);
        result.put("fp", fp);
        result.put("fn", fn);
        result.put("tn", tn);
        result.put("station", composed);
        return result;
    }

    private TelemetryEventDto entityToDto(TelemetryEventEntity entity) {
        List<Double> mfcc = null;
        if (entity.getMfccFeatures() != null && !entity.getMfccFeatures().trim().isEmpty()) {
            try {
                mfcc = objectMapper.readValue(entity.getMfccFeatures(), new TypeReference<List<Double>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse mfccFeatures JSON: {}", entity.getMfccFeatures());
            }
        }

        return TelemetryEventDto.builder()
                .id(entity.getId())
                .stationId(entity.getStationId())
                .timestamp(entity.getTimestamp())
                .eventType(entity.getEventType())
                .batteryLevel(entity.getBatteryLevel())
                .batteryVoltage(entity.getBatteryVoltage() != null ? entity.getBatteryVoltage().doubleValue() : null)
                .soilMoisture(entity.getSoilMoisture() != null ? entity.getSoilMoisture().doubleValue() : null)
                .acousticScore(entity.getAcousticScore())
                .temperature(entity.getTemperature() != null ? entity.getTemperature().doubleValue() : null)
                .humidity(entity.getHumidity() != null ? entity.getHumidity().doubleValue() : null)
                .rssi(entity.getRssi())
                .snr(entity.getSnr() != null ? entity.getSnr().doubleValue() : null)
                .predictLevel(entity.getPredictLevel())
                .aiRiskScore(entity.getAiRiskScore())
                .isMoistureTriggered(entity.getIsMoistureTriggered())
                .mfccFeatures(mfcc)
                .validationOutcome(entity.getValidationOutcome())
                .validatedAt(entity.getValidatedAt())
                .validatedBy(entity.getValidatedBy())
                .validationNotes(entity.getValidationNotes())
                .build();
    }
}
