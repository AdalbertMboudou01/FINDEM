package com.memoire.assistant.controller;

import com.memoire.assistant.dto.PublicJobSummaryResponse;
import com.memoire.assistant.model.Job;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class PublicJobController {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @GetMapping("/slug/{slug}")
    public ResponseEntity<PublicJobSummaryResponse> getJobBySlug(@PathVariable String slug) {
        Job job = jobRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Offre introuvable"));
        int candidateCount = applicationRepository.findByJob_JobId(job.getJobId()).size();
        boolean isAccepting = isJobAccepting(job, candidateCount);
        return ResponseEntity.ok(new PublicJobSummaryResponse(
                job.getJobId(), job.getTitle(), isAccepting, candidateCount, job.getMaxCandidatures()));
    }

    @GetMapping("/{jobId}/public")
    public ResponseEntity<PublicJobSummaryResponse> getPublicJobSummary(@PathVariable UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Offre introuvable"));

        int candidateCount = applicationRepository.findByJob_JobId(jobId).size();

        boolean isAccepting = isJobAccepting(job, candidateCount);

        return ResponseEntity.ok(new PublicJobSummaryResponse(
                job.getJobId(),
                job.getTitle(),
                isAccepting,
                candidateCount,
                job.getMaxCandidatures()
        ));
    }

    private boolean isJobAccepting(Job job, int currentCount) {
        // L'offre est fermée si son statut est explicitement "cloture"
        if ("cloture".equalsIgnoreCase(job.getStatut())) {
            return false;
        }
        // Si un quota est défini et autoClose activé, on vérifie
        if (job.getMaxCandidatures() != null && job.isAutoClose()) {
            return currentCount < job.getMaxCandidatures();
        }
        return true;
    }
}
