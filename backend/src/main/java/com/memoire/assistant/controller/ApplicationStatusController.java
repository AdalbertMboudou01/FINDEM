package com.memoire.assistant.controller;

import com.memoire.assistant.model.ApplicationStatus;
import com.memoire.assistant.service.ApplicationStatusService;
import com.memoire.assistant.dto.ApplicationStatusCreateRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/application-statuses")
public class ApplicationStatusController {
    @Autowired
    private ApplicationStatusService applicationStatusService;

    @GetMapping
    public List<ApplicationStatus> getAllStatuses() {
        return applicationStatusService.getAllStatuses();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationStatus> getStatusById(@PathVariable UUID id) {
        Optional<ApplicationStatus> status = applicationStatusService.getStatusById(id);
        return status.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ApplicationStatus createStatus(@Valid @RequestBody ApplicationStatusCreateRequest request) {
        ApplicationStatus status = new ApplicationStatus();
        status.setCode(request.getCode());
        status.setLabel(request.getLabel());
        return applicationStatusService.saveStatus(status);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApplicationStatus> updateStatus(@PathVariable UUID id, @RequestBody ApplicationStatus status) {
        if (!applicationStatusService.getStatusById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        status.setStatusId(id);
        return ResponseEntity.ok(applicationStatusService.saveStatus(status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStatus(@PathVariable UUID id) {
        if (!applicationStatusService.getStatusById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        applicationStatusService.deleteStatus(id);
        return ResponseEntity.noContent().build();
    }
}
