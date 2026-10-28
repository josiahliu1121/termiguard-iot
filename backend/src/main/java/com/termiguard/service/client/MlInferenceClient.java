package com.termiguard.service.client;

import com.termiguard.dto.MlPredictionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MlInferenceClient {

    private final RestTemplate restTemplate;
    private final String mlServiceUrl;

    public MlInferenceClient(RestTemplateBuilder restTemplateBuilder,
                             @Value("${termiguard.ml-service.url:http://localhost:5000}") String mlServiceUrl) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(1200))
                .setReadTimeout(Duration.ofMillis(2000))
                .build();
        this.mlServiceUrl = mlServiceUrl;
    }

    public MlPredictionResult predict(String stationId,
                                      BigDecimal soilMoisture,
                                      BigDecimal temperature,
                                      BigDecimal humidity,
                                      Integer acousticScore,
                                      List<BigDecimal> mfccFeatures) {
        // If acoustic score is null (heartbeat mode), return Safe immediately
        if (acousticScore == null) {
            return MlPredictionResult.builder()
                    .predictLevel("Safe")
                    .aiRiskScore(10)
                    .confidence(0.99)
                    .fallbackUsed(false)
                    .build();
        }

        try {
            Map<String, Object> requestPayload = new HashMap<>();
            requestPayload.put("stationId", stationId);
            requestPayload.put("soilMoisture", soilMoisture);
            requestPayload.put("temperature", temperature);
            requestPayload.put("humidity", humidity);
            requestPayload.put("acousticScore", acousticScore);
            requestPayload.put("mfccFeatures", mfccFeatures);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestPayload, headers);

            String endpoint = mlServiceUrl + "/predict";
            MlPredictionResult result = restTemplate.postForObject(endpoint, entity, MlPredictionResult.class);
            if (result != null && result.getPredictLevel() != null) {
                result.setFallbackUsed(false);
                return result;
            }
        } catch (Exception ex) {
            log.warn("FastAPI ML microservice call to {} failed ({}). Utilizing heuristic fallback.", 
                    mlServiceUrl + "/predict", ex.getMessage());
        }

        return fallbackHeuristic(acousticScore, soilMoisture);
    }

    private MlPredictionResult fallbackHeuristic(Integer acousticScore, BigDecimal soilMoisture) {
        String level;
        int riskScore;

        if (acousticScore >= 75) {
            level = "High";
            riskScore = Math.min(98, 75 + (int) ((acousticScore - 75) * 0.9));
        } else if (acousticScore >= 50) {
            level = "Medium";
            riskScore = 50 + (int) ((acousticScore - 50) * 0.9);
        } else if (acousticScore >= 25) {
            level = "Low";
            riskScore = 25 + (int) ((acousticScore - 25) * 0.9);
        } else {
            level = "Safe";
            riskScore = Math.max(5, (int)(acousticScore * 0.5));
        }

        return MlPredictionResult.builder()
                .predictLevel(level)
                .aiRiskScore(riskScore)
                .confidence(0.85)
                .fallbackUsed(true)
                .build();
    }
}
