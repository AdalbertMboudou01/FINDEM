package com.memoire.assistant.controller;

import com.memoire.assistant.dto.JobSettingsRequest;
import com.memoire.assistant.dto.JobSettingsResponse;
import com.memoire.assistant.model.Job;
import com.memoire.assistant.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobSettingsController {

    @Autowired
    private JobRepository jobRepository;

    @GetMapping("/{jobId}/settings")
    public ResponseEntity<JobSettingsResponse> getSettings(@PathVariable UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Offre introuvable"));

        return ResponseEntity.ok(new JobSettingsResponse(
                job.getJobId(),
                job.getMaxCandidatures(),
                job.isAutoClose()
        ));
    }

    @PatchMapping("/{jobId}/settings")
    public ResponseEntity<JobSettingsResponse> updateSettings(
            @PathVariable UUID jobId,
            @RequestBody JobSettingsRequest request) {

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Offre introuvable"));

        job.setMaxCandidatures(request.getMaxCandidatures());
        job.setAutoClose(request.isAutoClose());
        jobRepository.save(job);

        return ResponseEntity.ok(new JobSettingsResponse(
                job.getJobId(),
                job.getMaxCandidatures(),
                job.isAutoClose()
        ));
    }
}
