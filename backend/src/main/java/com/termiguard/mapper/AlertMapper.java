package com.termiguard.mapper;

import com.termiguard.entity.AlertEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AlertMapper {

    @Select("SELECT * FROM alerts ORDER BY timestamp DESC")
    List<AlertEntity> selectAll();

    @Select("SELECT * FROM alerts WHERE id = #{id}")
    AlertEntity selectById(@Param("id") String id);

    @Select("SELECT * FROM alerts WHERE station_id = #{stationId} AND status NOT IN ('resolved', 'false_alarm') ORDER BY timestamp DESC LIMIT 1")
    AlertEntity selectActiveByStationId(@Param("stationId") String stationId);

    @Insert("INSERT INTO alerts (" +
            "id, station_id, station_name, code, timestamp, confidence_tier, status, sop_stage, " +
            "summary, activity_score, technician_notes, verification_date, treatment_date, " +
            "toxicant_batch, resolution_date, validation_outcome, probable_cause, verified_by" +
            ") VALUES (" +
            "#{id}, #{stationId}, #{stationName}, #{code}, #{timestamp}, #{confidenceTier}, #{status}, #{sopStage}, " +
            "#{summary}, #{activityScore}, #{technicianNotes}, #{verificationDate}, #{treatmentDate}, " +
            "#{toxicantBatch}, #{resolutionDate}, #{validationOutcome}, #{probableCause}, #{verifiedBy}" +
            ")")
    int insert(AlertEntity alert);

    @Update("<script>" +
            "UPDATE alerts " +
            "<set>" +
            "  <if test='confidenceTier != null'>confidence_tier = #{confidenceTier},</if>" +
            "  <if test='status != null'>status = #{status},</if>" +
            "  <if test='sopStage != null'>sop_stage = #{sopStage},</if>" +
            "  <if test='summary != null'>summary = #{summary},</if>" +
            "  <if test='activityScore != null'>activity_score = #{activityScore},</if>" +
            "  <if test='technicianNotes != null'>technician_notes = #{technicianNotes},</if>" +
            "  <if test='verificationDate != null'>verification_date = #{verificationDate},</if>" +
            "  <if test='treatmentDate != null'>treatment_date = #{treatmentDate},</if>" +
            "  <if test='toxicantBatch != null'>toxicant_batch = #{toxicantBatch},</if>" +
            "  <if test='resolutionDate != null'>resolution_date = #{resolutionDate},</if>" +
            "  <if test='validationOutcome != null'>validation_outcome = #{validationOutcome},</if>" +
            "  <if test='probableCause != null'>probable_cause = #{probableCause},</if>" +
            "  <if test='verifiedBy != null'>verified_by = #{verifiedBy},</if>" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int update(AlertEntity alert);
}
