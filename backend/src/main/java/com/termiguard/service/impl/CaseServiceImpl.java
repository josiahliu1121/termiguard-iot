package com.termiguard.service.impl;

import com.termiguard.dto.CaseCreateRequest;
import com.termiguard.dto.CaseTransitionRequest;
import com.termiguard.dto.StationCompositeDto;
import com.termiguard.entity.AlertEntity;
import com.termiguard.entity.StationEntity;
import com.termiguard.entity.TelemetryEventEntity;
import com.termiguard.entity.VerificationCaseEntity;
import com.termiguard.mapper.AlertMapper;
import com.termiguard.mapper.StationMapper;
import com.termiguard.mapper.VerificationCaseMapper;
import com.termiguard.service.CaseService;
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

@Slf4j
@Service
public class CaseServiceImpl implements CaseService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final VerificationCaseMapper caseMapper;
    private final StationMapper stationMapper;
    private final AlertMapper alertMapper;
    private final EventService eventService;
    private final StationService stationService;
    private final TelemetryWebSocketHandler webSocketHandler;

    public CaseServiceImpl(VerificationCaseMapper caseMapper,
                           StationMapper stationMapper,
                           AlertMapper alertMapper,
                           @Lazy EventService eventService,
                           @Lazy StationService stationService,
                           TelemetryWebSocketHandler webSocketHandler) {
        this.caseMapper = caseMapper;
        this.stationMapper = stationMapper;
        this.alertMapper = alertMapper;
        this.eventService = eventService;
        this.stationService = stationService;
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public List<VerificationCaseEntity> getAllCases() {
        return caseMapper.selectAll();
    }

    @Override
    public VerificationCaseEntity getCaseById(String id) {
        return caseMapper.selectById(id);
    }

    @Override
    public List<VerificationCaseEntity> getCasesByStation(String stationId) {
        return caseMapper.selectByStationId(stationId);
    }

    @Override
    public VerificationCaseEntity getActiveCaseByStation(String stationId) {
        return caseMapper.selectActiveByStationId(stationId);
    }

    @Override
    @Transactional
    public Map<String, Object> createCase(CaseCreateRequest request) {
        StationEntity st = stationMapper.selectById(request.getStationId());
        if (st == null) {
            throw new RuntimeException("Station " + request.getStationId() + " not found");
        }

        String nowIso = LocalDateTime.now().format(DATE_FORMATTER);
        int totalCases = caseMapper.countTotal();
        String caseId = "CASE-2026-" + String.format("%04d", totalCases + 42);

        // 1. Retroactively label preceding 7 days of events
        Map<String, Object> stats = eventService.applyGroundTruthValidation(
                request.getStationId(),
                request.getFinding(),
                request.getTechnician(),
                request.getNotes()
        );

        String caseStatus;
        String outcome;
        boolean isHighOrMed = "High".equalsIgnoreCase(st.getThreatLevel()) || "Medium".equalsIgnoreCase(st.getThreatLevel());

        if ("termites_present".equalsIgnoreCase(request.getFinding())) {
            caseStatus = "field_verified_infestation";
            outcome = isHighOrMed ? "TP" : "FN";

            AlertEntity existingAlert = alertMapper.selectActiveByStationId(request.getStationId());
            if (existingAlert != null) {
                existingAlert.setStatus("verified_active");
                existingAlert.setSopStage("treat");
                existingAlert.setTechnicianNotes(request.getNotes());
                existingAlert.setVerifiedBy(request.getTechnician());
                existingAlert.setVerificationDate(nowIso);
                alertMapper.update(existingAlert);
            } else {
                AlertEntity newAlert = AlertEntity.builder()
                        .id("ALT-SOP-" + String.format("%04d", System.currentTimeMillis() % 10000))
                        .stationId(request.getStationId())
                        .stationName(st.getName())
                        .code(st.getCode())
                        .timestamp(nowIso)
                        .confidenceTier(st.getThreatLevel())
                        .status("verified_active")
                        .sopStage("treat")
                        .activityScore(st.getThreatIndex())
                        .summary("[PHYSICAL AUDIT VERIFIED] Field inspection confirmed live Coptotermes infestation on "
                                + st.getTreeTag() + ". Response protocol initiated.")
                        .technicianNotes("Reported by " + request.getTechnician() + ": " + request.getNotes())
                        .verificationDate(nowIso)
                        .verifiedBy(request.getTechnician())
                        .validationOutcome(outcome)
                        .probableCause(request.getProbableCause())
                        .build();
                alertMapper.insert(newAlert);

                Map<String, Object> alertWs = new HashMap<>();
                alertWs.put("type", "alert_triggered");
                alertWs.put("alert", newAlert);
                webSocketHandler.broadcast(alertWs);
            }
        } else {
            // Clean finding
            caseStatus = isHighOrMed ? "false_alarm" : "routine_clean";
            outcome = isHighOrMed ? "FP" : "TN";

            AlertEntity existingAlert = alertMapper.selectActiveByStationId(request.getStationId());
            if (existingAlert != null) {
                existingAlert.setStatus(isHighOrMed ? "false_alarm" : "resolved");
                existingAlert.setSopStage("closed");
                existingAlert.setResolutionDate(nowIso);
                String prevNotes = existingAlert.getTechnicianNotes() != null ? existingAlert.getTechnicianNotes() : "";
                existingAlert.setTechnicianNotes(prevNotes + "\n[Audit Cleared]: " + request.getNotes());
                alertMapper.update(existingAlert);
            }
        }

        VerificationCaseEntity newCase = VerificationCaseEntity.builder()
                .id(caseId)
                .stationId(request.getStationId())
                .stationCode(st.getCode())
                .stationName(st.getName())
                .createdAt(nowIso)
                .status(caseStatus)
                .technician(request.getTechnician())
                .finding(request.getFinding())
                .probableCause(request.getProbableCause())
                .initialNotes(request.getNotes())
                .verifiedAt(nowIso)
                .validationOutcome(outcome)
                .build();

        if ("routine_clean".equalsIgnoreCase(caseStatus) || "false_alarm".equalsIgnoreCase(caseStatus)) {
            newCase.setClearedAt(nowIso);
            newCase.setClearedNotes(request.getNotes());
        }

        caseMapper.insert(newCase);

        StationCompositeDto composed = stationService.getStationById(request.getStationId());

        Map<String, Object> stationWs = new HashMap<>();
        stationWs.put("type", "station_updated");
        stationWs.put("station", composed);
        webSocketHandler.broadcast(stationWs);

        Map<String, Object> result = new HashMap<>();
        result.put("caseRecord", newCase);
        result.put("station", composed);
        result.put("stats", stats);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> transitionCase(String caseId, CaseTransitionRequest request) {
        VerificationCaseEntity caseItem = caseMapper.selectById(caseId);
        if (caseItem == null) {
            throw new RuntimeException("Verification case " + caseId + " not found");
        }

        StationEntity station = stationMapper.selectById(caseItem.getStationId());
        if (station == null) {
            throw new RuntimeException("Station " + caseItem.getStationId() + " not found");
        }

        String nowIso = LocalDateTime.now().format(DATE_FORMATTER);
        String nextStatus = request.getNextStatus();
        caseItem.setStatus(nextStatus);

        AlertEntity alert = alertMapper.selectActiveByStationId(caseItem.getStationId());

        if ("treatment_start".equalsIgnoreCase(nextStatus)) {
            caseItem.setTreatmentStartedAt(nowIso);
            caseItem.setToxicantBatch(request.getToxicantBatch() != null ? request.getToxicantBatch() : "CHITIN-CSI-2026-BATCH-09");
            caseItem.setTreatmentNotes(request.getNotes() != null ? request.getNotes() : "In-ground toxicant placed at bait chamber.");
            station.setHumanVerificationStatus("treatment_start");

            if (alert != null) {
                alert.setSopStage("monitor");
                alert.setStatus("in_treatment");
                alert.setToxicantBatch(caseItem.getToxicantBatch());
                alert.setTreatmentDate(nowIso);
                String prev = alert.getTechnicianNotes() != null ? alert.getTechnicianNotes() : "";
                alert.setTechnicianNotes(prev + "\n[Treated]: " + caseItem.getTreatmentNotes());
                alertMapper.update(alert);
            }
        } else if ("monitor".equalsIgnoreCase(nextStatus)) {
            caseItem.setMonitoredAt(nowIso);
            caseItem.setMonitoringNotes(request.getNotes() != null ? request.getNotes() : "40-day colony elimination monitoring commenced.");
            station.setHumanVerificationStatus("monitor");

            if (alert != null) {
                alert.setSopStage("monitor");
                String prev = alert.getTechnicianNotes() != null ? alert.getTechnicianNotes() : "";
                alert.setTechnicianNotes(prev + "\n[Monitoring]: " + caseItem.getMonitoringNotes());
                alertMapper.update(alert);
            }
        } else if ("cleared".equalsIgnoreCase(nextStatus)) {
            caseItem.setClearedAt(nowIso);
            caseItem.setClearedNotes(request.getNotes() != null ? request.getNotes() : "Colony fully eradicated; sensor returned to normal baseline.");
            station.setHumanVerificationStatus("verified_clean");
            station.setVerifiedCleanAt(nowIso);

            if (alert != null) {
                alert.setStatus("resolved");
                alert.setSopStage("closed");
                alert.setResolutionDate(nowIso);
                String prev = alert.getTechnicianNotes() != null ? alert.getTechnicianNotes() : "";
                alert.setTechnicianNotes(prev + "\n[Cleared]: " + caseItem.getClearedNotes());
                alertMapper.update(alert);
            }

            // Insert a resolving heartbeat event into event table
            TelemetryEventEntity resolveEvent = TelemetryEventEntity.builder()
                    .stationId(caseItem.getStationId())
                    .timestamp(nowIso)
                    .eventType("daily_heartbeat")
                    .batteryLevel(92)
                    .batteryVoltage(BigDecimal.valueOf(4.0))
                    .soilMoisture(BigDecimal.valueOf(56.0))
                    .acousticScore(null)
                    .temperature(BigDecimal.valueOf(29.0))
                    .humidity(BigDecimal.valueOf(78.0))
                    .rssi(-84)
                    .snr(BigDecimal.valueOf(7.8))
                    .predictLevel("Safe")
                    .aiRiskScore(10)
                    .isMoistureTriggered(false)
                    .build();
            eventService.insertEvent(resolveEvent);
        } else if ("terminated".equalsIgnoreCase(nextStatus)) {
            caseItem.setStatus("terminated");
            caseItem.setTerminatedAt(nowIso);
            caseItem.setTerminatedNotes(request.getNotes() != null ? request.getNotes() : "Case terminated early by field technician.");
            station.setHumanVerificationStatus("verified_clean");
            station.setVerifiedCleanAt(nowIso);
            station.setTechnicianNotes("[Case Terminated]: " + caseItem.getTerminatedNotes());

            if (alert != null) {
                alert.setStatus("resolved");
                alert.setSopStage("closed");
                alert.setResolutionDate(nowIso);
                String prev = alert.getTechnicianNotes() != null ? alert.getTechnicianNotes() : "";
                alert.setTechnicianNotes(prev + "\n[Case Terminated]: " + caseItem.getTerminatedNotes());
                alertMapper.update(alert);
            }
        }

        caseMapper.update(caseItem);
        stationMapper.update(station);

        StationCompositeDto composed = stationService.getStationById(caseItem.getStationId());

        Map<String, Object> stationWs = new HashMap<>();
        stationWs.put("type", "station_updated");
        stationWs.put("station", composed);
        webSocketHandler.broadcast(stationWs);

        Map<String, Object> result = new HashMap<>();
        result.put("caseRecord", caseItem);
        result.put("station", composed);
        return result;
    }
}
