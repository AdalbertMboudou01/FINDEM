package com.memoire.assistant.controller;

import com.memoire.assistant.dto.ApplicationActivityDTO;
import com.memoire.assistant.model.ApplicationActivity;
import com.memoire.assistant.service.ApplicationActivityService;
import com.memoire.assistant.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApplicationActivityController {

    @Autowired
    private ApplicationActivityService activityService;

    /**
     * GET /api/applications/{id}/activity
     * Timeline complète d'une candidature (ordre chronologique).
     */
    @GetMapping("/applications/{id}/activity")
    public ResponseEntity<List<ApplicationActivityDTO>> getActivityForApplication(@PathVariable UUID id) {
        requireCompanyId();
        List<ApplicationActivityDTO> result = activityService.getActivityForApplication(id)
                .stream()
                .map(ApplicationActivityDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/activity
     * Feed global de toutes les activités de l'entreprise (ordre antéchronologique).
     */
    @GetMapping("/activity")
    public ResponseEntity<List<ApplicationActivityDTO>> getCompanyFeed() {
        requireCompanyId();
        List<ApplicationActivityDTO> result = activityService.getCompanyFeed()
                .stream()
                .map(ApplicationActivityDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    private void requireCompanyId() {
        if (TenantContext.getCompanyId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Contexte entreprise manquant");
        }
    }
}
