package com.termiguard.service;

import com.termiguard.dto.AlertActionRequest;
import com.termiguard.entity.AlertEntity;

import java.util.List;

public interface AlertService {
    List<AlertEntity> getAllAlerts();
    AlertEntity getAlertById(String id);
    AlertEntity performAction(String alertId, AlertActionRequest request);
}
