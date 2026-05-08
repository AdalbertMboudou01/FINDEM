package com.memoire.assistant.controller;

import com.memoire.assistant.dto.CandidateSummaryDTO;
import com.memoire.assistant.model.Candidate;
import com.memoire.assistant.service.PoolManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pool")
@Tag(name = "Pool Management", description = "Gestion du vivier de candidats")
public class PoolManagementController {
    
    @Autowired
    private PoolManagementService poolManagementService;
    
    @PostMapping("/add/{candidateId}")
    @Operation(summary = "Ajouter un candidat au vivier", description = "Ajoute un candidat au vivier pour des opportunités futures")
    public ResponseEntity<Candidate> addToPool(
            @PathVariable UUID candidateId,
            @RequestParam(required = false) String reason) {
        
        try {
            Candidate updatedCandidate = poolManagementService.addToPool(candidateId, reason);
            return ResponseEntity.ok(updatedCandidate);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/remove/{candidateId}")
    @Operation(summary = "Retirer un candidat du vivier", description = "Retire un candidat du vivier")
    public ResponseEntity<Candidate> removeFromPool(@PathVariable UUID candidateId) {
        try {
            Candidate updatedCandidate = poolManagementService.removeFromPool(candidateId);
            return ResponseEntity.ok(updatedCandidate);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/candidates")
    @Operation(summary = "Lister les candidats du vivier", description = "Retourne tous les candidats actuellement dans le vivier")
    public ResponseEntity<List<CandidateSummaryDTO>> getPoolCandidates() {
        List<CandidateSummaryDTO> candidates = poolManagementService.getPoolCandidates();
        return ResponseEntity.ok(candidates);
    }
    
    @GetMapping("/search")
    @Operation(summary = "Rechercher dans le vivier", description = "Recherche des candidats dans le vivier par mots-clés, compétences ou localisation")
    public ResponseEntity<List<CandidateSummaryDTO>> searchPoolCandidates(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String location) {
        
        List<CandidateSummaryDTO> candidates = poolManagementService.searchPoolCandidates(keyword, skill, location);
        return ResponseEntity.ok(candidates);
    }
    
    @GetMapping("/skill/{skill}")
    @Operation(summary = "Rechercher par compétence", description = "Retourne les candidats du vivier ayant une compétence spécifique")
    public ResponseEntity<List<CandidateSummaryDTO>> getPoolCandidatesBySkill(@PathVariable String skill) {
        List<CandidateSummaryDTO> candidates = poolManagementService.getPoolCandidatesBySkill(skill);
        return ResponseEntity.ok(candidates);
    }
    
    @GetMapping("/location/{location}")
    @Operation(summary = "Rechercher par localisation", description = "Retourne les candidats du vivier dans une localisation spécifique")
    public ResponseEntity<List<CandidateSummaryDTO>> getPoolCandidatesByLocation(@PathVariable String location) {
        List<CandidateSummaryDTO> candidates = poolManagementService.getPoolCandidatesByLocation(location);
        return ResponseEntity.ok(candidates);
    }
    
    @PostMapping("/reactivate/{candidateId}/job/{jobId}")
    @Operation(summary = "Réactiver un candidat pour une offre", description = "Réactive un candidat du vivier pour une nouvelle offre d'emploi")
    public ResponseEntity<Void> reactivateCandidateForJob(
            @PathVariable UUID candidateId,
            @PathVariable UUID jobId) {
        
        try {
            poolManagementService.reactivateCandidateForJob(candidateId, jobId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "Statistiques du vivier", description = "Retourne des statistiques sur les candidats dans le vivier")
    public ResponseEntity<PoolManagementService.PoolStatistics> getPoolStatistics() {
        PoolManagementService.PoolStatistics stats = poolManagementService.getPoolStatistics();
        return ResponseEntity.ok(stats);
    }
}
