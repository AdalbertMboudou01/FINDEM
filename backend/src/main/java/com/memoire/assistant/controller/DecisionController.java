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

    /** Lister les avis sur une candidature */
    @GetMapping("/decision-inputs")
    public ResponseEntity<List<DecisionInputDTO>> getInputs(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(decisionService.getInputs(applicationId));
    }

    /** Ajouter un avis individuel */
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

    /** Lire l'état décision (avis + décision finale si présente) */
    @GetMapping("/decision")
    public ResponseEntity<DecisionDTO> getDecision(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(decisionService.getDecision(applicationId));
    }

    /** Enregistrer la décision finale */
    @PostMapping("/decision")
    public ResponseEntity<DecisionDTO> recordDecision(
            @PathVariable UUID applicationId,
            @RequestBody Map<String, String> body) {
        String finalStatus = body.get("finalStatus");
        String rationale = body.get("rationale");
        return ResponseEntity.ok(decisionService.recordFinalDecision(applicationId, finalStatus, rationale));
    }
}
