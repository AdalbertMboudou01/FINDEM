package com.memoire.assistant.controller;

import com.memoire.assistant.model.Candidate;
import com.memoire.assistant.service.CandidateService;
import com.memoire.assistant.dto.CandidateCreateRequest;
import com.memoire.assistant.dto.CandidateSummaryDTO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {
    @Autowired
    private CandidateService candidateService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER') or hasRole('MANAGER')")
    public List<Candidate> getAllCandidates() {
        return candidateService.getAllCandidates();
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER') or hasRole('MANAGER')")
    public ResponseEntity<Candidate> getCandidateById(@PathVariable UUID id) {
        Optional<Candidate> candidate = candidateService.getCandidateById(id);
        return candidate.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER') or hasRole('MANAGER')")
    public Candidate createCandidate(@Valid @RequestBody CandidateCreateRequest request) {
        Candidate candidate = new Candidate();
        candidate.setFirstName(request.getFirstName());
        candidate.setLastName(request.getLastName());
        candidate.setEmail(request.getEmail());
        candidate.setPhone(request.getPhone());
        candidate.setLocation(request.getLocation());
        candidate.setSchool(request.getSchool());
        candidate.setConsent(request.getConsent());
        candidate.setGithubUrl(request.getGithubUrl());
        candidate.setPortfolioUrl(request.getPortfolioUrl());
        return candidateService.saveCandidate(candidate);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER') or hasRole('MANAGER')")
    public ResponseEntity<Candidate> updateCandidate(@PathVariable UUID id, @RequestBody Candidate candidate) {
        if (!candidateService.getCandidateById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        candidate.setCandidateId(id);
        Optional<Candidate> existingOpt = candidateService.getCandidateById(id);
        if (existingOpt.isPresent()) {
            Candidate existing = existingOpt.get();
            if (candidate.getGithubUrl() == null) candidate.setGithubUrl(existing.getGithubUrl());
            if (candidate.getPortfolioUrl() == null) candidate.setPortfolioUrl(existing.getPortfolioUrl());
        }
        return ResponseEntity.ok(candidateService.saveCandidate(candidate));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER') or hasRole('MANAGER')")
    public ResponseEntity<Void> deleteCandidate(@PathVariable UUID id) {
        if (!candidateService.getCandidateById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        candidateService.deleteCandidate(id);
        return ResponseEntity.noContent().build();
    }

    // --- Gestion du vivier ---
    @PostMapping("/{id}/pool")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER') or hasRole('MANAGER')")
    public ResponseEntity<Candidate> addToPool(@PathVariable UUID id) {
        Optional<Candidate> candidateOpt = candidateService.getCandidateById(id);
        if (candidateOpt.isEmpty()) return ResponseEntity.notFound().build();
        Candidate candidate = candidateOpt.get();
        candidate.setInPool(true);
        return ResponseEntity.ok(candidateService.saveCandidate(candidate));
    }

    @DeleteMapping("/{id}/pool")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER') or hasRole('MANAGER')")
    public ResponseEntity<Candidate> removeFromPool(@PathVariable UUID id) {
        Optional<Candidate> candidateOpt = candidateService.getCandidateById(id);
        if (candidateOpt.isEmpty()) return ResponseEntity.notFound().build();
        Candidate candidate = candidateOpt.get();
        candidate.setInPool(false);
        return ResponseEntity.ok(candidateService.saveCandidate(candidate));
    }

    @GetMapping("/pool")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER') or hasRole('MANAGER')")
    public List<Candidate> getPoolCandidates() {
        return candidateService.getPoolCandidates();
    }

    // --- Synthèse candidat ---
    @GetMapping("/{id}/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER') or hasRole('MANAGER')")
    public ResponseEntity<CandidateSummaryDTO> getCandidateSummary(@PathVariable UUID id) {
        Optional<CandidateSummaryDTO> summary = candidateService.getCandidateSummary(id);
        return summary.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

}
