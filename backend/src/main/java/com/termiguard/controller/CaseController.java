package com.termiguard.controller;

import com.termiguard.dto.CaseCreateRequest;
import com.termiguard.dto.CaseTransitionRequest;
import com.termiguard.entity.VerificationCaseEntity;
import com.termiguard.service.CaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cases")
@CrossOrigin(origins = "*")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @GetMapping
    public ResponseEntity<List<VerificationCaseEntity>> getAllCases() {
        return ResponseEntity.ok(caseService.getAllCases());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VerificationCaseEntity> getCaseById(@PathVariable String id) {
        VerificationCaseEntity caseEntity = caseService.getCaseById(id);
        if (caseEntity == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(caseEntity);
    }

    @GetMapping("/station/{stationId}")
    public ResponseEntity<List<VerificationCaseEntity>> getCasesByStation(@PathVariable String stationId) {
        return ResponseEntity.ok(caseService.getCasesByStation(stationId));
    }

    @GetMapping("/active/{stationId}")
    public ResponseEntity<VerificationCaseEntity> getActiveCaseByStation(@PathVariable String stationId) {
        VerificationCaseEntity activeCase = caseService.getActiveCaseByStation(stationId);
        return ResponseEntity.ok(activeCase);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCase(@RequestBody CaseCreateRequest request) {
        return ResponseEntity.ok(caseService.createCase(request));
    }

    @PutMapping("/{id}/transition")
    public ResponseEntity<Map<String, Object>> transitionCase(
            @PathVariable String id,
            @RequestBody CaseTransitionRequest request) {
        return ResponseEntity.ok(caseService.transitionCase(id, request));
    }
}
