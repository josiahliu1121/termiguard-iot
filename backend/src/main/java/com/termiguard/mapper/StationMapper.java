package com.termiguard.mapper;

import com.termiguard.entity.StationEntity;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface StationMapper {

    @Select("SELECT * FROM stations ORDER BY id ASC")
    List<StationEntity> selectAll();

    @Select("SELECT * FROM stations WHERE id = #{id}")
    StationEntity selectById(@Param("id") String id);

    @Insert("INSERT INTO stations (" +
            "id, code, name, description, type, latitude, longitude, plantation_zone, tree_tag, " +
            "is_profile_complete, moisture_threshold, sampling_frequency_minutes, heartbeat_interval_hours, " +
            "is_solar_powered, installed_at, threat_index, threat_level, window_hits, window_total, " +
            "window_summary, last_evaluated_at, human_verification_status, last_verified_at, verified_clean_at, " +
            "verified_by, technician_notes" +
            ") VALUES (" +
            "#{id}, #{code}, #{name}, #{description}, #{type}, #{latitude}, #{longitude}, #{plantationZone}, #{treeTag}, " +
            "#{isProfileComplete}, #{moistureThreshold}, #{samplingFrequencyMinutes}, #{heartbeatIntervalHours}, " +
            "#{isSolarPowered}, #{installedAt}, #{threatIndex}, #{threatLevel}, #{windowHits}, #{windowTotal}, " +
            "#{windowSummary}, #{lastEvaluatedAt}, #{humanVerificationStatus}, #{lastVerifiedAt}, #{verifiedCleanAt}, " +
            "#{verifiedBy}, #{technicianNotes}" +
            ")")
    int insert(StationEntity station);

    @Update("<script>" +
            "UPDATE stations " +
            "<set>" +
            "  <if test='code != null'>code = #{code},</if>" +
            "  <if test='name != null'>name = #{name},</if>" +
            "  <if test='description != null'>description = #{description},</if>" +
            "  <if test='type != null'>type = #{type},</if>" +
            "  <if test='latitude != null'>latitude = #{latitude},</if>" +
            "  <if test='longitude != null'>longitude = #{longitude},</if>" +
            "  <if test='plantationZone != null'>plantation_zone = #{plantationZone},</if>" +
            "  <if test='treeTag != null'>tree_tag = #{treeTag},</if>" +
            "  <if test='isProfileComplete != null'>is_profile_complete = #{isProfileComplete},</if>" +
            "  <if test='moistureThreshold != null'>moisture_threshold = #{moistureThreshold},</if>" +
            "  <if test='samplingFrequencyMinutes != null'>sampling_frequency_minutes = #{samplingFrequencyMinutes},</if>" +
            "  <if test='heartbeatIntervalHours != null'>heartbeat_interval_hours = #{heartbeatIntervalHours},</if>" +
            "  <if test='isSolarPowered != null'>is_solar_powered = #{isSolarPowered},</if>" +
            "  <if test='installedAt != null'>installed_at = #{installedAt},</if>" +
            "  <if test='threatIndex != null'>threat_index = #{threatIndex},</if>" +
            "  <if test='threatLevel != null'>threat_level = #{threatLevel},</if>" +
            "  <if test='windowHits != null'>window_hits = #{windowHits},</if>" +
            "  <if test='windowTotal != null'>window_total = #{windowTotal},</if>" +
            "  <if test='windowSummary != null'>window_summary = #{windowSummary},</if>" +
            "  <if test='lastEvaluatedAt != null'>last_evaluated_at = #{lastEvaluatedAt},</if>" +
            "  <if test='humanVerificationStatus != null'>human_verification_status = #{humanVerificationStatus},</if>" +
            "  <if test='lastVerifiedAt != null'>last_verified_at = #{lastVerifiedAt},</if>" +
            "  <if test='verifiedCleanAt != null'>verified_clean_at = #{verifiedCleanAt},</if>" +
            "  <if test='verifiedBy != null'>verified_by = #{verifiedBy},</if>" +
            "  <if test='technicianNotes != null'>technician_notes = #{technicianNotes},</if>" +
            "</set>" +
            "WHERE id = #{id}" +
            "</script>")
    int update(StationEntity station);

    @Select("SELECT COUNT(*) FROM stations")
    int countTotal();

    @Select("SELECT COUNT(*) FROM stations WHERE threat_level != 'Safe'")
    int countActiveThreats();
}
