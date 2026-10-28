package com.termiguard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.termiguard.dto.UplinkTelemetryRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class TelemetryUplinkIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should ingest 15-min triggered acoustic packet and return sync config")
    void testTriggeredPacketIngestion() throws Exception {
        UplinkTelemetryRequest request = UplinkTelemetryRequest.builder()
                .stationId("ST-01")
                .eventType("15min_triggered_event")
                .batteryLevel(88)
                .batteryVoltage(BigDecimal.valueOf(3.92))
                .soilMoisture(BigDecimal.valueOf(72.5))
                .acousticScore(84)
                .temperature(BigDecimal.valueOf(29.1))
                .humidity(BigDecimal.valueOf(83.4))
                .rssi(-76)
                .snr(BigDecimal.valueOf(7.8))
                .isMoistureTriggered(true)
                .mfccFeatures(Arrays.asList(
                        BigDecimal.valueOf(-10.2), BigDecimal.valueOf(14.5), BigDecimal.valueOf(8.1),
                        BigDecimal.valueOf(6.3), BigDecimal.valueOf(4.2), BigDecimal.valueOf(3.1),
                        BigDecimal.valueOf(2.5), BigDecimal.valueOf(1.9), BigDecimal.valueOf(1.2),
                        BigDecimal.valueOf(0.8), BigDecimal.valueOf(0.6), BigDecimal.valueOf(0.4),
                        BigDecimal.valueOf(0.2)
                ))
                .build();

        mockMvc.perform(post("/api/v1/telemetry/uplink")
                        .header("X-Node-API-Key", "termiguard-secret-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.stationId").value("ST-01"))
                .andExpect(jsonPath("$.eventId", notNullValue()))
                .andExpect(jsonPath("$.serverTime", notNullValue()))
                .andExpect(jsonPath("$.config.moistureThreshold").value(65.0))
                .andExpect(jsonPath("$.config.samplingFrequencyMinutes").value(15))
                .andExpect(jsonPath("$.config.heartbeatIntervalHours").value(24));
    }

    @Test
    @DisplayName("Should ingest 24-hr daily heartbeat dormant packet")
    void testHeartbeatPacketIngestion() throws Exception {
        UplinkTelemetryRequest request = UplinkTelemetryRequest.builder()
                .stationId("ST-02")
                .eventType("daily_heartbeat")
                .batteryLevel(96)
                .batteryVoltage(BigDecimal.valueOf(4.08))
                .soilMoisture(BigDecimal.valueOf(45.2))
                .acousticScore(null)
                .temperature(BigDecimal.valueOf(27.4))
                .humidity(BigDecimal.valueOf(68.0))
                .rssi(-82)
                .snr(BigDecimal.valueOf(9.0))
                .isMoistureTriggered(false)
                .mfccFeatures(null)
                .build();

        mockMvc.perform(post("/api/v1/telemetry/uplink")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.stationId").value("ST-02"))
                .andExpect(jsonPath("$.eventId", notNullValue()));
    }

    @Test
    @DisplayName("Should auto-discover and register newly deployed node on first uplink")
    void testAutoDiscoveryOnUplink() throws Exception {
        String newStationId = "ST-TEST-" + System.currentTimeMillis();
        UplinkTelemetryRequest request = UplinkTelemetryRequest.builder()
                .stationId(newStationId)
                .eventType("15min_triggered_event")
                .batteryLevel(100)
                .batteryVoltage(BigDecimal.valueOf(4.18))
                .soilMoisture(BigDecimal.valueOf(68.0))
                .acousticScore(55)
                .temperature(BigDecimal.valueOf(28.0))
                .humidity(BigDecimal.valueOf(75.0))
                .rssi(-70)
                .snr(BigDecimal.valueOf(10.0))
                .isMoistureTriggered(true)
                .build();

        mockMvc.perform(post("/api/v1/telemetry/uplink")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.stationId").value(newStationId))
                .andExpect(jsonPath("$.config.moistureThreshold").value(65.0));
    }
}
