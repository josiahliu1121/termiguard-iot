package com.termiguard.service;

import com.termiguard.dto.*;
import com.termiguard.entity.StationEntity;

import java.util.List;
import java.util.Map;

public interface StationService {
    List<StationCompositeDto> getAllStations();
    StationCompositeDto getStationById(String id);
    StationCompositeDto modifyStation(String id, StationModifyRequest request);
    StationCompositeDto createStation(StationCreateRequest request);
    Map<String, Object> reportFalseNegative(String id, FalseNegativeReportRequest request);
    Map<String, Object> logRoutineInspection(String id, RoutineInspectionRequest request);
    StationCompositeDto composeStation(StationEntity info, TelemetryEventDto latestEvent);
}
