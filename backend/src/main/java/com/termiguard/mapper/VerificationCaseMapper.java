package com.termiguard.mapper;

import com.termiguard.entity.VerificationCaseEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface VerificationCaseMapper {

    @Select("SELECT * FROM verification_cases ORDER BY created_at DESC")
    List<VerificationCaseEntity> selectAll();

    @Select("SELECT * FROM verification_cases WHERE id = #{id}")
    VerificationCaseEntity selectById(@Param("id") String id);

    @Select("SELECT * FROM verification_cases WHERE station_id = #{stationId} ORDER BY created_at DESC")
    List<VerificationCaseEntity> selectByStationId(@Param("stationId") String stationId);

    @Select("SELECT * FROM verification_cases " +
            "WHERE station_id = #{stationId} " +
            "  AND status NOT IN ('cleared', 'routine_clean', 'false_alarm', 'terminated') " +
            "ORDER BY created_at DESC LIMIT 1")
    VerificationCaseEntity selectActiveByStationId(@Param("stationId") String stationId);

    @Insert("INSERT INTO verification_cases (" +
            "id, station_id, station_code, station_name, created_at, status, technician, " +
            "finding, probable_cause, initial_notes, verified_at, treatment_started_at, toxicant_batch, " +
            "treatment_notes, monitored_at, monitoring_notes, cleared_at, cleared_notes, " +
            "terminated_at, terminated_notes, validation_outcome" +
            ") VALUES (" +
            "#{id}, #{stationId}, #{stationCode}, #{stationName}, #{createdAt}, #{status}, #{technician}, " +
            "#{finding}, #{probableCause}, #{initialNotes}, #{verifiedAt}, #{treatmentStartedAt}, #{toxicantBatch}, " +
            "#{treatmentNotes}, #{monitoredAt}, #{monitoringNotes}, #{clearedAt}, #{clearedNotes}, " +
            "#{terminatedAt}, #{terminatedNotes}, #{validationOutcome}" +
            ")")
    int insert(VerificationCaseEntity caseEntity);

    @Update("<script>" +
            "UPDATE verification_cases " +
            "<set>" +
            "  <if test='status != null'>status = #{status},</if>" +
            "  <if test='technician != null'>technician = #{technician},</if>" +
            "  <if test='finding != null'>finding = #{finding},</if>" +
            "  <if test='probableCause != null'>probable_cause = #{probableCause},</if>" +
            "  <if test='initialNotes != null'>initial_notes = #{initialNotes},</if>" +
            "  <if test='verifiedAt != null'>verified_at = #{verifiedAt},</if>" +
            "  <if test='treatmentStartedAt != null'>treatment_started_at = #{treatmentStartedAt},</if>" +
            "  <if test='toxicantBatch != null'>toxicant_batch = #{toxicantBatch},</if>" +
            "  <if test='treatmentNotes != null'>treatment_notes = #{treatmentNotes},</if>" +
            "  <if test='monitoredAt != null'>monitored_at = #{monitoredAt},</if>" +
            "  <if test='monitoringNotes != null'>monitoring_notes = #{monitoringNotes},</if>" +
            "  <if test='clearedAt != null'>cleared_at = #{clearedAt},</if>" +
            "  <if test='clearedNotes != null'>cleared_notes = #{clearedNotes},</if>" +
            "  <if test='terminatedAt != null'>terminated_at = #{terminatedAt},</if>" +
            "  <if test='terminatedNotes != null'>terminated_notes = #{terminatedNotes},</if>" +
            "  <if test='validationOutcome != null'>validation_outcome = #{validationOutcome},</if>" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int update(VerificationCaseEntity caseEntity);

    @Select("SELECT COUNT(*) FROM verification_cases")
    int countTotal();
}
