package com.memoire.assistant.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ApplicationCreateRequest {
    @NotNull(message = "L'identifiant du candidat est obligatoire")
    private UUID candidateId;
    @NotNull(message = "L'identifiant du job est obligatoire")
    private UUID jobId;
    @NotNull(message = "L'identifiant du statut est obligatoire")
    private UUID statusId;
    // Getters & Setters
    public UUID getCandidateId() { return candidateId; }
    public void setCandidateId(UUID candidateId) { this.candidateId = candidateId; }
    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public UUID getStatusId() { return statusId; }
    public void setStatusId(UUID statusId) { this.statusId = statusId; }
}
