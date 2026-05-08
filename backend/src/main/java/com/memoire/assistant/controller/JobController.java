package com.memoire.assistant.controller;

import com.memoire.assistant.model.Job;
import com.memoire.assistant.service.JobService;
import com.memoire.assistant.dto.JobCreateRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    @Autowired
    private JobService jobService;

    @GetMapping
    public List<Job> getAllJobs() {
        return jobService.getAllJobs();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable UUID id) {
        Optional<Job> job = jobService.getJobById(id);
        return job.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Job createJob(@Valid @RequestBody JobCreateRequest request) {
        return jobService.createJobFromRequest(request);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Job> updateJob(@PathVariable UUID id, @RequestBody Job job) {
        Optional<Job> existing = jobService.getJobById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        job.setCompany(existing.get().getCompany());
        job.setOwnerRecruiter(existing.get().getOwnerRecruiter());
        job.setJobId(id);
        return ResponseEntity.ok(jobService.saveJob(job));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable UUID id) {
        if (!jobService.getJobById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }
}
