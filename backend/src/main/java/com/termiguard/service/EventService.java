package com.termiguard.service;

import com.termiguard.dto.StationCompositeDto;
import com.termiguard.dto.TelemetryEventDto;
import com.termiguard.dto.UplinkTelemetryRequest;
import com.termiguard.dto.UplinkTelemetryResponse;
import com.termiguard.entity.TelemetryEventEntity;

import java.util.List;
import java.util.Map;

public interface EventService {
    List<TelemetryEventDto> getAllEvents();
    List<TelemetryEventDto> getEventsByStation(String stationId);
    TelemetryEventDto getLatestEventForStation(String stationId);
    TelemetryEventDto insertEvent(TelemetryEventEntity eventEntity);
    UplinkTelemetryResponse processUplink(UplinkTelemetryRequest request);
    Map<String, Object> applyGroundTruthValidation(String stationId, String finding, String technician, String notes);
    StationCompositeDto evaluateTemporalPersistence(String stationId);
}
