package com.termiguard.service.impl;

import com.termiguard.dto.*;
import com.termiguard.entity.AlertEntity;
import com.termiguard.entity.StationEntity;
import com.termiguard.mapper.AlertMapper;
import com.termiguard.mapper.StationMapper;
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
public class StationServiceImpl implements StationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StationMapper stationMapper;
    private final AlertMapper alertMapper;
    private final EventService eventService;
    private final TelemetryWebSocketHandler webSocketHandler;

    public StationServiceImpl(StationMapper stationMapper,
                              AlertMapper alertMapper,
                              @Lazy EventService eventService,
                              TelemetryWebSocketHandler webSocketHandler) {
        this.stationMapper = stationMapper;
        this.alertMapper = alertMapper;
        this.eventService = eventService;
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public List<StationCompositeDto> getAllStations() {
        List<StationEntity> stations = stationMapper.selectAll();
        return stations.stream().map(st -> {
            TelemetryEventDto latest = eventService.getLatestEventForStation(st.getId());
            return composeStation(st, latest);
        }).collect(Collectors.toList());
    }

    @Override
    public StationCompositeDto getStationById(String id) {
        StationEntity entity = stationMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        TelemetryEventDto latest = eventService.getLatestEventForStation(id);
        return composeStation(entity, latest);
    }

    @Override
    @Transactional
    public StationCompositeDto modifyStation(String id, StationModifyRequest request) {
        StationEntity existing = stationMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("Station " + id + " not found");
        }

        if (request.getName() != null) existing.setName(request.getName());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());
        if (request.getType() != null) existing.setType(request.getType());
        if (request.getPlantationZone() != null) existing.setPlantationZone(request.getPlantationZone());
        if (request.getTreeTag() != null) existing.setTreeTag(request.getTreeTag());
        if (request.getIsProfileComplete() != null) existing.setIsProfileComplete(request.getIsProfileComplete());
        if (request.getMoistureThreshold() != null) existing.setMoistureThreshold(BigDecimal.valueOf(request.getMoistureThreshold()));
        if (request.getSamplingFrequencyMinutes() != null) existing.setSamplingFrequencyMinutes(request.getSamplingFrequencyMinutes());
        if (request.getHeartbeatIntervalHours() != null) existing.setHeartbeatIntervalHours(request.getHeartbeatIntervalHours());
        if (request.getIsSolarPowered() != null) existing.setIsSolarPowered(request.getIsSolarPowered());
        if (request.getThreatIndex() != null) existing.setThreatIndex(request.getThreatIndex());
        if (request.getThreatLevel() != null) existing.setThreatLevel(request.getThreatLevel());
        if (request.getWindowHits() != null) existing.setWindowHits(request.getWindowHits());
        if (request.getWindowTotal() != null) existing.setWindowTotal(request.getWindowTotal());
        if (request.getWindowSummary() != null) existing.setWindowSummary(request.getWindowSummary());
        if (request.getHumanVerificationStatus() != null) existing.setHumanVerificationStatus(request.getHumanVerificationStatus());
        if (request.getTechnicianNotes() != null) existing.setTechnicianNotes(request.getTechnicianNotes());

        if (request.getCoordinates() != null && request.getCoordinates().size() >= 2) {
            existing.setLatitude(BigDecimal.valueOf(request.getCoordinates().get(0)));
            existing.setLongitude(BigDecimal.valueOf(request.getCoordinates().get(1)));
        }

        stationMapper.update(existing);

        TelemetryEventDto latest = eventService.getLatestEventForStation(id);
        StationCompositeDto composed = composeStation(existing, latest);

        // Broadcast station_updated
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("type", "station_updated");
        wsPayload.put("station", composed);
        webSocketHandler.broadcast(wsPayload);

        return composed;
    }

    @Override
    @Transactional
    public StationCompositeDto createStation(StationCreateRequest request) {
        String now = LocalDateTime.now().format(DATE_FORMATTER);
        String id = request.getId() != null ? request.getId() : "ST-" + String.format("%02d", stationMapper.countTotal());

        BigDecimal lat = BigDecimal.valueOf(2.5898);
        BigDecimal lng = BigDecimal.valueOf(102.0931);
        if (request.getCoordinates() != null && request.getCoordinates().size() >= 2) {
            lat = BigDecimal.valueOf(request.getCoordinates().get(0));
            lng = BigDecimal.valueOf(request.getCoordinates().get(1));
        }

        StationEntity entity = StationEntity.builder()
                .id(id)
                .code(request.getCode() != null ? request.getCode() : id)
                .name(request.getName() != null ? request.getName() : "Station " + id)
                .description(request.getDescription() != null ? request.getDescription() : "Auto-registered from initial telemetry packet.")
                .type(request.getType() != null ? request.getType() : "peripheral")
                .latitude(lat)
                .longitude(lng)
                .plantationZone(request.getPlantationZone() != null ? request.getPlantationZone() : "")
                .treeTag(request.getTreeTag() != null ? request.getTreeTag() : "")
                .isProfileComplete(request.getIsProfileComplete() != null ? request.getIsProfileComplete() : false)
                .moistureThreshold(request.getMoistureThreshold() != null ? BigDecimal.valueOf(request.getMoistureThreshold()) : BigDecimal.valueOf(65.0))
                .samplingFrequencyMinutes(request.getSamplingFrequencyMinutes() != null ? request.getSamplingFrequencyMinutes() : 15)
                .heartbeatIntervalHours(request.getHeartbeatIntervalHours() != null ? request.getHeartbeatIntervalHours() : 24)
                .isSolarPowered("master".equalsIgnoreCase(request.getType()))
                .installedAt(request.getInstalledAt() != null ? request.getInstalledAt() : now)
                .threatIndex(request.getThreatIndex() != null ? request.getThreatIndex() : 10)
                .threatLevel(request.getThreatLevel() != null ? request.getThreatLevel() : "Safe")
                .windowHits(request.getWindowHits() != null ? request.getWindowHits() : 0)
                .windowTotal(request.getWindowTotal() != null ? request.getWindowTotal() : 1)
                .windowSummary(request.getWindowSummary() != null ? request.getWindowSummary() : "Initial telemetry online")
                .lastEvaluatedAt(now)
                .humanVerificationStatus("unverified")
                .build();

        stationMapper.insert(entity);
        StationCompositeDto composed = composeStation(entity, null);

        // Broadcast station_updated
        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("type", "station_updated");
        wsPayload.put("station", composed);
        webSocketHandler.broadcast(wsPayload);

        return composed;
    }

    @Override
    @Transactional
    public Map<String, Object> reportFalseNegative(String id, FalseNegativeReportRequest request) {
        StationEntity st = stationMapper.selectById(id);
        if (st == null) {
            throw new RuntimeException("Station " + id + " not found");
        }

        String technician = (request != null && request.getTechnician() != null && !request.getTechnician().isEmpty())
                ? request.getTechnician() : "Field Technician";
        String notes = (request != null && request.getNotes() != null)
                ? request.getNotes() : "Field inspection confirmed live termite presence unflagged by sensor.";
        String probableCause = (request != null && request.getProbableCause() != null)
                ? request.getProbableCause() : "mudpack_outside_chamber";

        // 1. Retroactively label previous 7 days
        Map<String, Object> stats = eventService.applyGroundTruthValidation(id, "termites_present", technician, notes);

        String now = LocalDateTime.now().format(DATE_FORMATTER);
        String alertId = "ALT-FN-" + String.format("%04d", System.currentTimeMillis() % 10000);

        AlertEntity alert = AlertEntity.builder()
                .id(alertId)
                .stationId(id)
                .stationName(st.getName())
                .code(st.getCode())
                .timestamp(now)
                .confidenceTier("High")
                .status("verified_active")
                .sopStage("treat")
                .activityScore(st.getThreatIndex() != null ? st.getThreatIndex() : 60)
                .summary("[FALSE NEGATIVE DETECTED] Human field patrol discovered active Coptotermes subterranean termites on "
                        + st.getTreeTag() + " unflagged by automated threshold. Escalated directly to treatment SOP.")
                .technicianNotes("Reported by " + technician + ": " + notes)
                .verificationDate(now)
                .verifiedBy(technician)
                .validationOutcome("FN")
                .probableCause(probableCause)
                .build();

        alertMapper.insert(alert);

        StationCompositeDto stationDto = getStationById(id);

        // Broadcast alert and station
        Map<String, Object> alertWs = new HashMap<>();
        alertWs.put("type", "alert_triggered");
        alertWs.put("alert", alert);
        webSocketHandler.broadcast(alertWs);

        Map<String, Object> stationWs = new HashMap<>();
        stationWs.put("type", "station_updated");
        stationWs.put("station", stationDto);
        webSocketHandler.broadcast(stationWs);

        Map<String, Object> result = new HashMap<>();
        result.put("alert", alert);
        result.put("station", stationDto);
        result.put("stats", stats);
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> logRoutineInspection(String id, RoutineInspectionRequest request) {
        String technician = (request != null && request.getTechnician() != null && !request.getTechnician().isEmpty())
                ? request.getTechnician() : "Field Technician";
        String notes = (request != null && request.getNotes() != null)
                ? request.getNotes() : "Routine patrol: Clean tree, zero mudpack, healthy bark.";

        Map<String, Object> stats = eventService.applyGroundTruthValidation(id, "clean_no_termites", technician, notes);
        StationCompositeDto stationDto = getStationById(id);

        Map<String, Object> stationWs = new HashMap<>();
        stationWs.put("type", "station_updated");
        stationWs.put("station", stationDto);
        webSocketHandler.broadcast(stationWs);

        Map<String, Object> result = new HashMap<>();
        result.put("station", stationDto);
        result.put("stats", stats);
        return result;
    }

    @Override
    public StationCompositeDto composeStation(StationEntity info, TelemetryEventDto latestEvent) {
        boolean isAcousticGated = latestEvent == null
                || latestEvent.getAcousticScore() == null
                || (latestEvent.getSoilMoisture() != null && info.getMoistureThreshold() != null
                && latestEvent.getSoilMoisture() < info.getMoistureThreshold().doubleValue());

        Double[] coords = new Double[]{
                info.getLatitude() != null ? info.getLatitude().doubleValue() : 2.5898,
                info.getLongitude() != null ? info.getLongitude().doubleValue() : 102.0915
        };

        String zone = info.getPlantationZone() != null && !info.getPlantationZone().isEmpty()
                ? info.getPlantationZone()
                : (Boolean.FALSE.equals(info.getIsProfileComplete()) ? "Awaiting Zone Setup" : "Zone A");

        String status = "High".equalsIgnoreCase(info.getThreatLevel()) ? "warning" : "online";
        String confidenceTier = info.getThreatLevel() != null ? info.getThreatLevel()
                : (latestEvent != null && latestEvent.getPredictLevel() != null ? latestEvent.getPredictLevel() : "Safe");
        String operatingMode = isAcousticGated ? "dormant_heartbeat" : "acoustic_triggered";
        Double soilMoisture = latestEvent != null && latestEvent.getSoilMoisture() != null ? latestEvent.getSoilMoisture() : 55.0;
        Integer batteryLevel = latestEvent != null && latestEvent.getBatteryLevel() != null ? latestEvent.getBatteryLevel() : 95;
        Double batteryVoltage = latestEvent != null && latestEvent.getBatteryVoltage() != null ? latestEvent.getBatteryVoltage() : 4.0;
        Double temperature = latestEvent != null && latestEvent.getTemperature() != null ? latestEvent.getTemperature() : 29.5;
        Double humidity = latestEvent != null && latestEvent.getHumidity() != null ? latestEvent.getHumidity() : 78.0;
        Integer activityScore = latestEvent != null ? latestEvent.getAcousticScore() : null;
        Integer aiRiskScore = info.getThreatIndex() != null ? info.getThreatIndex()
                : (latestEvent != null && latestEvent.getAiRiskScore() != null ? latestEvent.getAiRiskScore() : 12);
        Integer rssi = latestEvent != null && latestEvent.getRssi() != null ? latestEvent.getRssi() : -80;
        Double snr = latestEvent != null && latestEvent.getSnr() != null ? latestEvent.getSnr() : 8.0;

        String lastSeen = "No events";
        if (latestEvent != null && latestEvent.getTimestamp() != null) {
            if ("15min_triggered_event".equals(latestEvent.getEventType())) {
                String[] parts = latestEvent.getTimestamp().split(" ");
                String timePart = parts.length > 1 && parts[1].length() >= 5 ? parts[1].substring(0, 5) : "";
                lastSeen = timePart + " (15m Event)";
            } else {
                String[] parts = latestEvent.getTimestamp().split(" ");
                lastSeen = parts[0] + " (Daily Heartbeat)";
            }
        }

        String lastHeartbeatTime = latestEvent != null && latestEvent.getTimestamp() != null ? latestEvent.getTimestamp() : info.getInstalledAt();
        String lastAcousticCaptureTime = !isAcousticGated && latestEvent != null ? latestEvent.getTimestamp() : null;
        String nextExpectedTransmission = isAcousticGated ? "Tomorrow 06:00 (Daily Heartbeat)" : "In 15 mins (15m Window)";

        return StationCompositeDto.builder()
                .id(info.getId())
                .code(info.getCode())
                .name(info.getName())
                .description(info.getDescription())
                .type(info.getType())
                .coordinates(coords)
                .plantationZone(info.getPlantationZone())
                .treeTag(info.getTreeTag())
                .isProfileComplete(info.getIsProfileComplete() != null ? info.getIsProfileComplete() : true)
                .moistureThreshold(info.getMoistureThreshold() != null ? info.getMoistureThreshold().doubleValue() : 65.0)
                .samplingFrequencyMinutes(info.getSamplingFrequencyMinutes())
                .heartbeatIntervalHours(info.getHeartbeatIntervalHours())
                .isSolarPowered(info.getIsSolarPowered() != null ? info.getIsSolarPowered() : false)
                .installedAt(info.getInstalledAt())
                .threatIndex(info.getThreatIndex())
                .threatLevel(info.getThreatLevel())
                .windowHits(info.getWindowHits())
                .windowTotal(info.getWindowTotal())
                .windowSummary(info.getWindowSummary())
                .lastEvaluatedAt(info.getLastEvaluatedAt())
                .humanVerificationStatus(info.getHumanVerificationStatus())
                .lastVerifiedAt(info.getLastVerifiedAt())
                .verifiedCleanAt(info.getVerifiedCleanAt())
                .verifiedBy(info.getVerifiedBy())
                .technicianNotes(info.getTechnicianNotes())
                .zone(zone)
                .status(status)
                .confidenceTier(confidenceTier)
                .operatingMode(operatingMode)
                .isAcousticGated(isAcousticGated)
                .soilMoisture(soilMoisture)
                .batteryLevel(batteryLevel)
                .batteryVoltage(batteryVoltage)
                .temperature(temperature)
                .humidity(humidity)
                .activityScore(activityScore)
                .aiRiskScore(aiRiskScore)
                .rssi(rssi)
                .snr(snr)
                .lastSeen(lastSeen)
                .lastHeartbeatTime(lastHeartbeatTime)
                .lastAcousticCaptureTime(lastAcousticCaptureTime)
                .nextExpectedTransmission(nextExpectedTransmission)
                .latestEvent(latestEvent)
                .mfccFeatures(latestEvent != null ? latestEvent.getMfccFeatures() : null)
                .build();
    }
}
