package com.termiguard.mapper;

import com.termiguard.entity.TelemetryEventEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface TelemetryEventMapper {

    @Select("SELECT * FROM events ORDER BY timestamp DESC")
    List<TelemetryEventEntity> selectAll();

    @Select("SELECT * FROM events WHERE station_id = #{stationId} ORDER BY timestamp ASC")
    List<TelemetryEventEntity> selectByStationId(@Param("stationId") String stationId);

    @Select("SELECT * FROM events WHERE station_id = #{stationId} ORDER BY timestamp DESC LIMIT 1")
    TelemetryEventEntity selectLatestByStationId(@Param("stationId") String stationId);

    @Select("SELECT * FROM events ORDER BY timestamp DESC LIMIT 1")
    TelemetryEventEntity selectLatestOverall();

    @Insert("INSERT INTO events (" +
            "id, station_id, timestamp, event_type, battery_level, battery_voltage, soil_moisture, " +
            "acoustic_score, temperature, humidity, rssi, snr, predict_level, ai_risk_score, " +
            "is_moisture_triggered, mfcc_features, validation_outcome, validated_at, validated_by, validation_notes" +
            ") VALUES (" +
            "#{id}, #{stationId}, #{timestamp}, #{eventType}, #{batteryLevel}, #{batteryVoltage}, #{soilMoisture}, " +
            "#{acousticScore}, #{temperature}, #{humidity}, #{rssi}, #{snr}, #{predictLevel}, #{aiRiskScore}, " +
            "#{isMoistureTriggered}, #{mfccFeatures}, #{validationOutcome}, #{validatedAt}, #{validatedBy}, #{validationNotes}" +
            ")")
    int insert(TelemetryEventEntity event);

    @Update("UPDATE events SET " +
            "validation_outcome = #{outcome}, " +
            "validated_at = #{validatedAt}, " +
            "validated_by = #{validatedBy}, " +
            "validation_notes = #{notes} " +
            "WHERE id = #{id}")
    int updateValidationOutcome(@Param("id") String id,
                               @Param("outcome") String outcome,
                               @Param("validatedAt") String validatedAt,
                               @Param("validatedBy") String validatedBy,
                               @Param("notes") String notes);

    @Select("SELECT * FROM events " +
            "WHERE station_id = #{stationId} " +
            "  AND timestamp >= #{cutoffTime} " +
            "  AND timestamp <= #{nowTime} " +
            "ORDER BY timestamp ASC")
    List<TelemetryEventEntity> selectEventsForValidation(@Param("stationId") String stationId,
                                                         @Param("cutoffTime") String cutoffTime,
                                                         @Param("nowTime") String nowTime);

    @Select("SELECT " +
            "SUM(CASE WHEN validation_outcome = 'TP' THEN 1 ELSE 0 END) as tp, " +
            "SUM(CASE WHEN validation_outcome = 'FP' THEN 1 ELSE 0 END) as fp, " +
            "SUM(CASE WHEN validation_outcome = 'FN' THEN 1 ELSE 0 END) as fn, " +
            "SUM(CASE WHEN validation_outcome = 'TN' THEN 1 ELSE 0 END) as tn " +
            "FROM events")
    Map<String, Object> selectConfusionMatrixCounts();
}
