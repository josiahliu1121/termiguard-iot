package com.termiguard.service;

import com.termiguard.dto.CaseCreateRequest;
import com.termiguard.dto.CaseTransitionRequest;
import com.termiguard.entity.VerificationCaseEntity;

import java.util.List;
import java.util.Map;

public interface CaseService {
    List<VerificationCaseEntity> getAllCases();
    VerificationCaseEntity getCaseById(String id);
    List<VerificationCaseEntity> getCasesByStation(String stationId);
    VerificationCaseEntity getActiveCaseByStation(String stationId);
    Map<String, Object> createCase(CaseCreateRequest request);
    Map<String, Object> transitionCase(String caseId, CaseTransitionRequest request);
}
