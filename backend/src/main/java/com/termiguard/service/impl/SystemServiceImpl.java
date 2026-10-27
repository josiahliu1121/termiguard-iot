package com.termiguard.service.impl;

import com.termiguard.dto.GroundTruthMetricsDto;
import com.termiguard.dto.SystemStatusDto;
import com.termiguard.entity.GatewayInfoEntity;
import com.termiguard.entity.TelemetryEventEntity;
import com.termiguard.mapper.GatewayInfoMapper;
import com.termiguard.mapper.StationMapper;
import com.termiguard.mapper.TelemetryEventMapper;
import com.termiguard.service.SystemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Slf4j
@Service
public class SystemServiceImpl implements SystemService {

    private final GatewayInfoMapper gatewayInfoMapper;
    private final StationMapper stationMapper;
    private final TelemetryEventMapper eventMapper;

    public SystemServiceImpl(GatewayInfoMapper gatewayInfoMapper,
                             StationMapper stationMapper,
                             TelemetryEventMapper eventMapper) {
        this.gatewayInfoMapper = gatewayInfoMapper;
        this.stationMapper = stationMapper;
        this.eventMapper = eventMapper;
    }

    @Override
    public SystemStatusDto getSystemStatus() {
        GatewayInfoEntity gw = gatewayInfoMapper.selectFirst();
        int total = stationMapper.countTotal();
        TelemetryEventEntity latestEv = eventMapper.selectLatestOverall();

        return SystemStatusDto.builder()
                .gatewayOnline(gw != null ? gw.getIsOnline() : true)
                .gatewayBackhaul(gw != null && gw.getBackhaul() != null ? gw.getBackhaul() : "5G")
                .masterConnected(true)
                .activeStationsCount(total)
                .totalStationsCount(total)
                .aiInferenceServiceOnline(true)
                .timescaleDbHealthy(true)
                .lastPacketTime(latestEv != null ? latestEv.getTimestamp() : "2026-10-21 20:30:00")
                .build();
    }

    @Override
    public GroundTruthMetricsDto getGroundTruthMetrics() {
        Map<String, Object> counts = eventMapper.selectConfusionMatrixCounts();

        int tp = getIntVal(counts != null ? counts.get("tp") : 0);
        int fp = getIntVal(counts != null ? counts.get("fp") : 0);
        int fn = getIntVal(counts != null ? counts.get("fn") : 0);
        int tn = getIntVal(counts != null ? counts.get("tn") : 0);

        int total = tp + fp + fn + tn;

        double precision = (tp + fp) > 0 ? roundDouble(((double) tp / (tp + fp)) * 100.0) : 100.0;
        double recall = (tp + fn) > 0 ? roundDouble(((double) tp / (tp + fn)) * 100.0) : 100.0;
        double accuracy = total > 0 ? roundDouble(((double) (tp + tn) / total) * 100.0) : 100.0;
        double falseAlarmRate = (fp + tn) > 0 ? roundDouble(((double) fp / (fp + tn)) * 100.0) : 0.0;
        double missRate = (tp + fn) > 0 ? roundDouble(((double) fn / (tp + fn)) * 100.0) : 0.0;

        return GroundTruthMetricsDto.builder()
                .tp(tp)
                .fp(fp)
                .fn(fn)
                .tn(tn)
                .total(total)
                .precision(precision)
                .recall(recall)
                .accuracy(accuracy)
                .falseAlarmRate(falseAlarmRate)
                .missRate(missRate)
                .build();
    }

    private int getIntVal(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private double roundDouble(double val) {
        return BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
