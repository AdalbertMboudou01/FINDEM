package com.memoire.assistant.controller;

import com.memoire.assistant.dto.ApplyRequest;
import com.memoire.assistant.dto.ApplyResponse;
import com.memoire.assistant.model.Application;
import com.memoire.assistant.model.Candidate;
import com.memoire.assistant.model.Job;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.CandidateRepository;
import com.memoire.assistant.repository.JobRepository;
import com.memoire.assistant.service.ApplicationService;
import com.memoire.assistant.service.ChatAnswerService;
import com.memoire.assistant.service.FileUploadService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/apply")
public class ApplyController {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private ChatAnswerService chatAnswerService;

    @PostMapping(consumes = {"application/json"})
    public ResponseEntity<ApplyResponse> apply(@Valid @RequestBody ApplyRequest request) {
        Candidate candidate = upsertCandidate(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPhone(),
                request.getSchool(),
                request.getConsent(),
                request.getGithubUrl(),
                request.getPortfolioUrl()
        );

        Application saved = createApplication(request.getJobId(), candidate);

        return ResponseEntity.ok(new ApplyResponse(candidate.getCandidateId(), saved.getApplicationId()));
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ApplyResponse> applyMultipart(
            @RequestParam("jobId") UUID jobId,
            @RequestParam("firstName") String firstName,
            @RequestParam("lastName") String lastName,
            @RequestParam("email") String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "school", required = false) String school,
            @RequestParam("consent") Boolean consent,
            @RequestParam(value = "githubUrl", required = false) String githubUrl,
            @RequestParam(value = "portfolioUrl", required = false) String portfolioUrl,
            @RequestParam(value = "cv", required = false) MultipartFile cv
    ) {
        Candidate candidate = upsertCandidate(
                firstName,
                lastName,
                email,
                phone,
                school,
                consent,
                githubUrl,
                portfolioUrl
        );

        if (cv != null && !cv.isEmpty()) {
            try {
                fileUploadService.uploadFile(cv, candidate.getCandidateId(), "CV");
            } catch (IOException e) {
                throw new RuntimeException("Erreur lors de l'upload du CV");
            }
        }

        Application saved = createApplication(jobId, candidate);

        return ResponseEntity.ok(new ApplyResponse(candidate.getCandidateId(), saved.getApplicationId()));
    }

    @PostMapping("/{applicationId}/finalize")
    public ResponseEntity<Map<String, String>> finalize(@PathVariable UUID applicationId) {
        CompletableFuture.runAsync(() -> {
            try {
                chatAnswerService.analyzeChatAnswers(applicationId);
            } catch (Exception ignored) {}
        });
        return ResponseEntity.ok(Map.of("status", "analysis_triggered"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntime(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    private Application createApplication(UUID jobId, Candidate candidate) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Offre introuvable"));

        // Return existing application if candidate already applied to this job
        return applicationRepository
                .findByCandidate_CandidateIdAndJob_JobId(candidate.getCandidateId(), jobId)
                .orElseGet(() -> {
                    Application application = new Application();
                    application.setCandidate(candidate);
                    application.setJob(job);
                    application.setCreatedAt(new java.util.Date());
                    return applicationService.saveApplication(application);
                });
    }

    private Candidate upsertCandidate(
            String firstName,
            String lastName,
            String email,
            String phone,
            String school,
            Boolean consent,
            String githubUrl,
            String portfolioUrl
    ) {
        if (firstName == null || firstName.trim().isEmpty() ||
                lastName == null || lastName.trim().isEmpty() ||
                email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Prénom, nom et email sont obligatoires");
        }
        if (consent == null || !consent) {
            throw new RuntimeException("Le consentement est obligatoire");
        }

        Candidate candidate = candidateRepository.findByEmail(email)
                .orElseGet(Candidate::new);

        candidate.setFirstName(firstName.trim());
        candidate.setLastName(lastName.trim());
        candidate.setEmail(email.trim());
        candidate.setPhone(phone);
        candidate.setSchool(school);
        candidate.setConsent(consent);
        candidate.setGithubUrl(githubUrl);
        candidate.setPortfolioUrl(portfolioUrl);

        return candidateRepository.save(candidate);
    }
}
