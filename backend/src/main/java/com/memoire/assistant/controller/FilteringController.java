package com.memoire.assistant.controller;

import com.memoire.assistant.dto.ApplicationSummaryDTO;
import com.memoire.assistant.service.FilteringEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/filtering")
@Tag(name = "Filtering Engine", description = "Moteur de filtrage des candidatures")
public class FilteringController {
    
    @Autowired
    private FilteringEngineService filteringEngineService;
    
    @PostMapping("/applications/{jobId}")
    @Operation(summary = "Filtrer les candidatures pour une offre", description = "Applique les critères de filtrage sur les candidatures d'une offre")
    public ResponseEntity<List<ApplicationSummaryDTO>> filterApplications(
            @PathVariable UUID jobId,
            @RequestBody FilteringEngineService.FilteringCriteria criteria) {
        
        try {
            List<ApplicationSummaryDTO> filteredApplications = filteringEngineService.filterApplications(jobId, criteria);
            return ResponseEntity.ok(filteredApplications);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/criteria-example")
    @Operation(summary = "Exemple de critères de filtrage", description = "Retourne un exemple de structure pour les critères de filtrage")
    public ResponseEntity<FilteringEngineService.FilteringCriteria> getFilteringCriteriaExample() {
        FilteringEngineService.FilteringCriteria example = new FilteringEngineService.FilteringCriteria();
        
        // Critères bloquants
        example.setBlockingCriteria(List.of("DISPONIBILITY", "LOCATION", "CONSENT"));
        
        // Niveaux minimums
        example.setMinMotivationLevel("MEDIUM");
        example.setMinTechnicalLevel("MEDIUM");
        
        // Filtres spécifiques
        example.setExperienceLevel("JUNIOR");
        example.setLocationMatch("PERFECT");
        example.setRecommendedAction("INTERVIEW");
        
        return ResponseEntity.ok(example);
    }
}
