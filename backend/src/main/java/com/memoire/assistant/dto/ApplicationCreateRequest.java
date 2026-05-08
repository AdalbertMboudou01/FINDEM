package com.memoire.assistant.dto;

import java.util.UUID;

public class ApplicationCreateRequest {
    private UUID candidateId;
    private UUID jobId;
    private UUID statusId;
    // Ajoute d'autres champs si besoin

    public UUID getCandidateId() {
        return candidateId;
    }
    public void setCandidateId(UUID candidateId) {
        this.candidateId = candidateId;
    }
    public UUID getJobId() {
        return jobId;
    }
    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }
    public UUID getStatusId() {
        return statusId;
    }
    public void setStatusId(UUID statusId) {
        this.statusId = statusId;
    }
}
