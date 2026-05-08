package com.memoire.assistant.controller;

import com.memoire.assistant.model.Interview;
import com.memoire.assistant.repository.InterviewRepository;
import com.memoire.assistant.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/interviews")
public class GlobalInterviewController {

    @Autowired private InterviewRepository interviewRepository;

    @GetMapping
    public ResponseEntity<List<Interview>> getMyInterviews() {
        UUID recruiterId = TenantContext.getRecruiterId();
        List<Interview> interviews = interviewRepository.findByRecruiter_RecruiterId(recruiterId);
        interviews.sort(Comparator.comparing(
            i -> i.getScheduledAt() != null ? i.getScheduledAt() : java.time.LocalDateTime.MAX
        ));
        return ResponseEntity.ok(interviews);
    }
}
