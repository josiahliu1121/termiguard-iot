package com.termiguard.mapper;

import com.termiguard.entity.GatewayInfoEntity;
import org.apache.ibatis.annotations.*;

@Mapper
public interface GatewayInfoMapper {

    @Select("SELECT * FROM gateway_info LIMIT 1")
    GatewayInfoEntity selectFirst();

    @Update("UPDATE gateway_info SET " +
            "name = #{name}, latitude = #{latitude}, longitude = #{longitude}, " +
            "backhaul = #{backhaul}, signal_dbm = #{signalDbm}, ip_address = #{ipAddress}, " +
            "is_online = #{isOnline} " +
            "WHERE id = #{id}")
    int update(GatewayInfoEntity gatewayInfo);
}
