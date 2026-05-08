package com.memoire.assistant.controller;

import com.memoire.assistant.model.Application;
import com.memoire.assistant.model.Interview;
import com.memoire.assistant.model.Recruiter;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.InterviewRepository;
import com.memoire.assistant.repository.RecruiterRepository;
import com.memoire.assistant.security.TenantContext;
import com.memoire.assistant.model.ApplicationActivity.EventType;
import com.memoire.assistant.service.ApplicationActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications/{applicationId}/interviews")
public class InterviewController {

    @Autowired private InterviewRepository interviewRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private RecruiterRepository recruiterRepository;
    @Autowired private ApplicationActivityService activityService;

    @GetMapping
    public ResponseEntity<List<Interview>> getInterviews(@PathVariable UUID applicationId) {
        Application app = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<Interview> interviews = interviewRepository.findByCandidateAndJob(
            app.getCandidate().getCandidateId(), app.getJob().getJobId());
        return ResponseEntity.ok(interviews);
    }

    @PostMapping
    public ResponseEntity<Interview> scheduleInterview(
            @PathVariable UUID applicationId,
            @RequestBody Map<String, Object> body) {

        Application app = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        UUID recruiterId = TenantContext.getRecruiterId();
        Recruiter recruiter = recruiterRepository.findById(recruiterId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recruteur introuvable"));

        Interview interview = new Interview();
        interview.setCandidate(app.getCandidate());
        interview.setJob(app.getJob());
        interview.setRecruiter(recruiter);
        interview.setTitle((String) body.getOrDefault("title", "Entretien"));
        interview.setDescription((String) body.get("description"));
        interview.setLocation((String) body.get("location"));
        interview.setMeetingUrl((String) body.get("meetingUrl"));
        interview.setNotes((String) body.get("notes"));
        interview.setStatus("SCHEDULED");

        Object durObj = body.get("durationMinutes");
        if (durObj instanceof Number n) interview.setDurationMinutes(n.intValue());

        String scheduledAtStr = (String) body.get("scheduledAt");
        if (scheduledAtStr != null && !scheduledAtStr.isBlank()) {
            interview.setScheduledAt(LocalDateTime.parse(scheduledAtStr));
        }

        Interview saved = interviewRepository.save(interview);

        activityService.logEvent(applicationId, EventType.INTERVIEW_SCHEDULED, Map.of(
            "title", saved.getTitle(),
            "scheduledAt", saved.getScheduledAt() != null ? saved.getScheduledAt().toString() : ""
        ));

        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/{interviewId}/status")
    public ResponseEntity<Interview> updateStatus(
            @PathVariable UUID applicationId,
            @PathVariable UUID interviewId,
            @RequestBody Map<String, String> body) {

        Interview interview = interviewRepository.findById(interviewId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String newStatus = body.get("status");
        if (newStatus != null) interview.setStatus(newStatus);

        return ResponseEntity.ok(interviewRepository.save(interview));
    }

    @DeleteMapping("/{interviewId}")
    public ResponseEntity<Void> deleteInterview(
            @PathVariable UUID applicationId,
            @PathVariable UUID interviewId) {

        interviewRepository.deleteById(interviewId);
        return ResponseEntity.noContent().build();
    }
}
