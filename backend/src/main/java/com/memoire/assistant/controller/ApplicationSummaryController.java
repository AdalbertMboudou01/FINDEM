package com.memoire.assistant.controller;

import com.memoire.assistant.dto.ApplicationSummaryDTO;
import com.memoire.assistant.model.ApplicationSummary;
import com.memoire.assistant.service.ApplicationSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/summaries")
@Tag(name = "Application Summaries", description = "Gestion des fiches synthétiques des candidatures")
public class ApplicationSummaryController {
    
    @Autowired
    private ApplicationSummaryService applicationSummaryService;
    
    @PostMapping("/generate/{applicationId}")
    @Operation(summary = "Générer une fiche synthétique", description = "Génère automatiquement une fiche synthétique pour une candidature")
    public ResponseEntity<ApplicationSummary> generateSummary(@PathVariable UUID applicationId) {
        try {
            ApplicationSummary summary = applicationSummaryService.generateSummary(applicationId);
            return ResponseEntity.ok(summary);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/application/{applicationId}")
    @Operation(summary = "Récupérer la fiche synthétique d'une candidature", description = "Retourne la fiche synthétique pour une candidature donnée")
    public ResponseEntity<ApplicationSummaryDTO> getSummaryByApplication(@PathVariable UUID applicationId) {
        ApplicationSummaryDTO summary = applicationSummaryService.getSummaryByApplication(applicationId);
        return summary != null ? ResponseEntity.ok(summary) : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/action/{recommendedAction}")
    @Operation(summary = "Récupérer les fiches par action recommandée", description = "Retourne toutes les fiches synthétiques ayant une action recommandée spécifique")
    public ResponseEntity<List<ApplicationSummaryDTO>> getSummariesByAction(@PathVariable String recommendedAction) {
        List<ApplicationSummaryDTO> summaries = applicationSummaryService.getSummariesByAction(recommendedAction);
        return ResponseEntity.ok(summaries);
    }
    
    @GetMapping("/actions")
    @Operation(summary = "Récupérer les fiches par actions recommandées", description = "Retourne les fiches synthétiques groupées par action recommandée")
    public ResponseEntity<?> getAllSummariesByActions() {
        // Récupérer toutes les actions possibles
        String[] actions = {"INTERVIEW", "REVIEW", "POOL", "REJECT"};
        
        java.util.Map<String, List<ApplicationSummaryDTO>> allSummaries = new java.util.HashMap<>();
        
        for (String action : actions) {
            List<ApplicationSummaryDTO> summaries = applicationSummaryService.getSummariesByAction(action);
            if (!summaries.isEmpty()) {
                allSummaries.put(action, summaries);
            }
        }
        
        return ResponseEntity.ok(allSummaries);
    }
}
