package com.memoire.assistant.controller;

import com.memoire.assistant.model.Application;
import com.memoire.assistant.model.ApplicationActivity.EventType;
import com.memoire.assistant.model.Candidate;
import com.memoire.assistant.service.ApplicationService;
import com.memoire.assistant.service.ApplicationActivityService;
import com.memoire.assistant.service.ApplicationStatusService;
import com.memoire.assistant.service.TeamViewService;
import com.memoire.assistant.service.WebSocketPublisher;
import com.memoire.assistant.dto.ApplicationCreateRequest;
import com.memoire.assistant.model.Job;
import com.memoire.assistant.model.ApplicationStatus;
import com.memoire.assistant.repository.CandidateRepository;
import com.memoire.assistant.repository.JobRepository;
import com.memoire.assistant.security.TenantContext;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ApplicationActivityService activityService;

    @Autowired
    private ApplicationStatusService applicationStatusService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private TeamViewService teamViewService;

    @Autowired
    private WebSocketPublisher webSocketPublisher;

    @GetMapping
    public ResponseEntity<?> getApplications(
            @RequestParam(required = false) String view,
            @RequestParam(required = false) UUID offerId) {
        if (view == null) {
            return ResponseEntity.ok(applicationService.getAllApplicationsForCurrentCompany());
        }
        return ResponseEntity.ok(teamViewService.getView(view, offerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Application> getApplicationById(@PathVariable UUID id) {
        Optional<Application> application = applicationService.getApplicationByIdForCurrentCompany(id);
        return application.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{applicationId}/allowed-transitions")
    public ResponseEntity<List<String>> getAllowedTransitions(@PathVariable UUID applicationId) {
        Optional<Application> applicationOpt = applicationService.getApplicationByIdForCurrentCompany(applicationId);
        if (applicationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Application application = applicationOpt.get();
        String currentCode = application.getStatus() != null ? application.getStatus().getCode() : null;
        List<String> transitions = applicationStatusService.getAllowedTransitions(currentCode == null ? "" : currentCode);
        return ResponseEntity.ok(transitions);
    }

    @PostMapping
    public Application createApplication(@Valid @RequestBody ApplicationCreateRequest request) {
        UUID companyId = requireCompanyId();

        Job persistedJob = jobRepository.findByJobIdAndCompany_CompanyId(request.getJobId(), companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Offre introuvable pour votre entreprise"));

        Candidate persistedCandidate = candidateRepository.findByCandidateIdAndCompanyId(request.getCandidateId(), companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Candidat introuvable pour votre entreprise"));

        Application application = new Application();
        application.setCandidate(persistedCandidate);
        application.setJob(persistedJob);
        ApplicationStatus status = new ApplicationStatus();
        status.setStatusId(request.getStatusId());
        application.setStatus(status);
        Application savedApplication = applicationService.saveApplication(application);

        activityService.logEvent(savedApplication.getApplicationId(), EventType.STATUS_CHANGED,
                Map.of("to", status.getStatusId() != null ? status.getStatusId().toString() : "initial",
                       "label", "Candidature créée"));

        return savedApplication;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Application> updateApplication(@PathVariable UUID id, @RequestBody Application application) {
        Optional<Application> existing = applicationService.getApplicationByIdForCurrentCompany(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        String previousStatusCode = existing.get().getStatus() != null ? existing.get().getStatus().getCode() : null;
        String previousStatusLabel = existing.get().getStatus() != null ? existing.get().getStatus().getLabel() : null;

        // Validate transition if status is changing
        if (application.getStatus() != null && application.getStatus().getStatusId() != null) {
            Optional<ApplicationStatus> newStatusOpt = applicationStatusService.getStatusById(application.getStatus().getStatusId());
            if (newStatusOpt.isPresent()) {
                String newCode = newStatusOpt.get().getCode();
                if (newCode != null && !newCode.equals(previousStatusCode)) {
                    try {
                        applicationStatusService.validateTransition(previousStatusCode, newCode, id);
                    } catch (IllegalStateException ex) {
                        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
                    }
                }
            }
        }

        application.setApplicationId(id);
        application.setCandidate(existing.get().getCandidate());
        application.setJob(existing.get().getJob());
        application.setCreatedAt(existing.get().getCreatedAt());
        Application updatedApplication = applicationService.saveApplication(application);
        String nextStatusLabel = updatedApplication.getStatus() != null ? updatedApplication.getStatus().getLabel() : null;

        if (previousStatusLabel != null && !previousStatusLabel.equals(nextStatusLabel)) {
            activityService.logEvent(id, EventType.STATUS_CHANGED,
                    Map.of("from", previousStatusLabel != null ? previousStatusLabel : "",
                           "to", nextStatusLabel != null ? nextStatusLabel : ""));
        }

        return ResponseEntity.ok(updatedApplication);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> patchStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        Optional<Application> existingOpt = applicationService.getApplicationByIdForCurrentCompany(id);
        if (existingOpt.isEmpty()) return ResponseEntity.notFound().build();

        String statusCode = body.get("statusCode");
        if (statusCode == null || statusCode.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "statusCode requis"));
        }

        Application existing = existingOpt.get();
        String previousCode = existing.getStatus() != null ? existing.getStatus().getCode() : null;

        List<ApplicationStatus> allStatuses = applicationStatusService.getAllStatuses();
        ApplicationStatus newStatus = allStatuses.stream()
                .filter(s -> statusCode.equals(s.getCode()))
                .findFirst()
                .orElse(null);
        if (newStatus == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Statut inconnu : " + statusCode));
        }

        try {
            applicationStatusService.validateTransition(previousCode, statusCode, id);
        } catch (IllegalStateException ex) {
            return ResponseEntity.unprocessableEntity().body(Map.of("message", ex.getMessage()));
        }

        existing.setStatus(newStatus);
        Application saved = applicationService.saveApplication(existing);

        activityService.logEvent(id, EventType.STATUS_CHANGED, Map.of(
                "from", previousCode != null ? previousCode : "",
                "to", statusCode));

        UUID companyId = TenantContext.getCompanyId();
        if (companyId != null) {
            webSocketPublisher.broadcastApplicationUpdate(companyId, Map.of(
                "applicationId", id.toString(),
                "statusCode", newStatus.getCode(),
                "statusLabel", newStatus.getLabel()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "applicationId", id.toString(),
                "statusCode", newStatus.getCode(),
                "statusLabel", newStatus.getLabel()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable UUID id) {
        Optional<Application> existing = applicationService.getApplicationByIdForCurrentCompany(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        applicationService.deleteApplication(id);
        return ResponseEntity.noContent().build();
    }

    private UUID requireCompanyId() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Contexte entreprise manquant");
        }
        return companyId;
    }
}
