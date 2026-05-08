package com.memoire.assistant.controller;

import com.memoire.assistant.model.Recruiter;
import com.memoire.assistant.security.TenantContext;
import com.memoire.assistant.service.RecruiterService;
import com.memoire.assistant.dto.RecruiterCreateRequest;
import com.memoire.assistant.dto.RecruiterUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/recruiters")
public class RecruiterController {
    @Autowired
    private RecruiterService recruiterService;

    @GetMapping
    public List<Recruiter> getAllRecruiters() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            return java.util.Collections.emptyList();
        }
        return recruiterService.getRecruitersByCompanyId(companyId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Recruiter> getRecruiterById(@PathVariable UUID id) {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            return ResponseEntity.notFound().build();
        }
        Optional<Recruiter> recruiter = recruiterService.getRecruiterByIdAndCompanyId(id, companyId);
        return recruiter.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Recruiter createRecruiter(@Valid @RequestBody RecruiterCreateRequest request) {
        throw new ResponseStatusException(
            HttpStatus.METHOD_NOT_ALLOWED,
            "Utilisez /api/company-members/invitations pour ajouter un recruteur"
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Recruiter> updateRecruiter(@PathVariable UUID id, @RequestBody Recruiter recruiter) {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null || !recruiterService.getRecruiterByIdAndCompanyId(id, companyId).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        recruiter.setRecruiterId(id);
        return ResponseEntity.ok(recruiterService.saveRecruiter(recruiter));
    }
    
    @PutMapping("/profile")
    public ResponseEntity<Recruiter> updateProfile(@Valid @RequestBody RecruiterUpdateRequest request) {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        // Récupérer le recruteur actuel
        Recruiter currentRecruiter = recruiterService.getRecruiterByAuthUserIdAndCompanyId(
            TenantContext.getUserId(), companyId)
            .orElseThrow(() -> new RuntimeException("Recruteur non trouvé"));
        
        // Mettre à jour les champs modifiables
        if (request.getName() != null) {
            currentRecruiter.setName(request.getName());
        }
        if (request.getEmail() != null) {
            currentRecruiter.setEmail(request.getEmail());
        }
        if (request.getPhotoUrl() != null) {
            currentRecruiter.setPhotoUrl(request.getPhotoUrl());
        }
        if (request.getBio() != null) {
            currentRecruiter.setBio(request.getBio());
        }
        if (request.getPhone() != null) {
            currentRecruiter.setPhone(request.getPhone());
        }
        
        Recruiter updatedRecruiter = recruiterService.saveRecruiter(currentRecruiter);
        return ResponseEntity.ok(updatedRecruiter);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecruiter(@PathVariable UUID id) {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null || !recruiterService.getRecruiterByIdAndCompanyId(id, companyId).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        recruiterService.deleteRecruiter(id);
        return ResponseEntity.noContent().build();
    }
}
