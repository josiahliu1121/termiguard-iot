package com.termiguard.service.impl;

import com.termiguard.dto.AlertActionRequest;
import com.termiguard.entity.AlertEntity;
import com.termiguard.entity.TelemetryEventEntity;
import com.termiguard.mapper.AlertMapper;
import com.termiguard.service.AlertService;
import com.termiguard.service.EventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class AlertServiceImpl implements AlertService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AlertMapper alertMapper;
    private final EventService eventService;

    public AlertServiceImpl(AlertMapper alertMapper,
                            @Lazy EventService eventService) {
        this.alertMapper = alertMapper;
        this.eventService = eventService;
    }

    @Override
    public List<AlertEntity> getAllAlerts() {
        return alertMapper.selectAll();
    }

    @Override
    public AlertEntity getAlertById(String id) {
        return alertMapper.selectById(id);
    }

    @Override
    @Transactional
    public AlertEntity performAction(String alertId, AlertActionRequest request) {
        AlertEntity alert = alertMapper.selectById(alertId);
        if (alert == null) {
            throw new RuntimeException("Alert " + alertId + " not found");
        }

        String nowIso = LocalDateTime.now().format(DATE_FORMATTER);
        String action = request.getAction();
        String notes = request.getNotes() != null ? request.getNotes() : "";
        String technician = (request.getTechnician() != null && !request.getTechnician().isEmpty())
                ? request.getTechnician() : "Ju Esen";

        if ("verify".equalsIgnoreCase(action)) {
            alert.setStatus("verified_active");
            alert.setSopStage("treat");
            alert.setVerificationDate(nowIso);
            alert.setVerifiedBy(technician);
            alert.setValidationOutcome("TP");
            alert.setTechnicianNotes(!notes.isEmpty() ? notes : "Technician confirmed active subterranean termite presence.");

            eventService.applyGroundTruthValidation(alert.getStationId(), "termites_present", alert.getVerifiedBy(), alert.getTechnicianNotes());
        } else if ("treat".equalsIgnoreCase(action)) {
            alert.setStatus("in_treatment");
            alert.setSopStage("monitor");
            alert.setTreatmentDate(nowIso);
            alert.setToxicantBatch(request.getToxicantBatch() != null ? request.getToxicantBatch() : "CHITIN-CSI-B2026-09");
            String prev = alert.getTechnicianNotes() != null ? alert.getTechnicianNotes() : "";
            alert.setTechnicianNotes(prev + "\n[Treated]: " + (!notes.isEmpty() ? notes : "In-ground toxicant placed."));
        } else if ("resolve".equalsIgnoreCase(action)) {
            alert.setStatus("resolved");
            alert.setSopStage("closed");
            alert.setResolutionDate(nowIso);
            String prev = alert.getTechnicianNotes() != null ? alert.getTechnicianNotes() : "";
            alert.setTechnicianNotes(prev + "\n[Resolved]: " + (!notes.isEmpty() ? notes : "Sensor returned to baseline; cleared."));

            // Insert baseline heartbeat event
            insertBaselineHeartbeat(alert.getStationId(), nowIso);
        } else if ("mark_false_alarm".equalsIgnoreCase(action)) {
            alert.setStatus("false_alarm");
            alert.setSopStage("closed");
            alert.setResolutionDate(nowIso);
            alert.setVerifiedBy(technician);
            alert.setValidationOutcome("FP");
            alert.setProbableCause(!notes.isEmpty() ? notes : "Non-biological artifact");
            String prev = alert.getTechnicianNotes() != null ? alert.getTechnicianNotes() : "";
            alert.setTechnicianNotes(prev + "\n[False Alarm]: " + (!notes.isEmpty() ? notes : "Non-biological artifact."));

            eventService.applyGroundTruthValidation(alert.getStationId(), "clean_no_termites", alert.getVerifiedBy(), alert.getTechnicianNotes());

            // Insert baseline heartbeat event
            insertBaselineHeartbeat(alert.getStationId(), nowIso);
        }

        alertMapper.update(alert);
        return alert;
    }

    private void insertBaselineHeartbeat(String stationId, String timestamp) {
        TelemetryEventEntity ev = TelemetryEventEntity.builder()
                .stationId(stationId)
                .timestamp(timestamp)
                .eventType("daily_heartbeat")
                .batteryLevel(90)
                .batteryVoltage(BigDecimal.valueOf(3.95))
                .soilMoisture(BigDecimal.valueOf(56.0))
                .acousticScore(null)
                .temperature(BigDecimal.valueOf(29.0))
                .humidity(BigDecimal.valueOf(78.0))
                .rssi(-85)
                .snr(BigDecimal.valueOf(7.5))
                .predictLevel("Safe")
                .aiRiskScore(12)
                .isMoistureTriggered(false)
                .build();
        eventService.insertEvent(ev);
    }
}
