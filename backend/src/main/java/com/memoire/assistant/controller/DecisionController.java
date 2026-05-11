package com.memoire.assistant.controller;

import com.memoire.assistant.dto.DecisionDTO;
import com.memoire.assistant.dto.DecisionInputDTO;
import com.memoire.assistant.service.DecisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications/{applicationId}")
public class DecisionController {

    @Autowired
    private DecisionService decisionService;

    @GetMapping("/decision-inputs")
    public ResponseEntity<List<DecisionInputDTO>> getInputs(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(decisionService.getInputs(applicationId));
    }

    @PostMapping("/decision-inputs")
    public ResponseEntity<DecisionInputDTO> addInput(
            @PathVariable UUID applicationId,
            @RequestBody Map<String, Object> body) {
        String sentiment = (String) body.get("sentiment");
        String comment = (String) body.get("comment");
        Integer confidence = body.get("confidence") != null
                ? ((Number) body.get("confidence")).intValue() : null;
        return ResponseEntity.ok(decisionService.addInput(applicationId, sentiment, comment, confidence));
    }

    @PatchMapping("/decision-inputs/{inputId}")
    public ResponseEntity<DecisionInputDTO> updateInput(
            @PathVariable UUID applicationId,
            @PathVariable UUID inputId,
            @RequestBody Map<String, Object> body) {
        String sentiment = (String) body.get("sentiment");
        String comment = (String) body.get("comment");
        Integer confidence = body.get("confidence") != null
                ? ((Number) body.get("confidence")).intValue() : null;
        return ResponseEntity.ok(decisionService.updateInput(applicationId, inputId, sentiment, comment, confidence));
    }

    @GetMapping("/decision")
    public ResponseEntity<DecisionDTO> getDecision(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(decisionService.getDecision(applicationId));
    }

    /** Décision finale — réservée au MANAGER */
    @PostMapping("/decision")
    public ResponseEntity<?> recordDecision(
            @PathVariable UUID applicationId,
            @RequestBody Map<String, String> body) {
        try {
            String finalStatus = body.get("finalStatus");
            String rationale = body.get("rationale");
            return ResponseEntity.ok(decisionService.recordFinalDecision(applicationId, finalStatus, rationale));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
