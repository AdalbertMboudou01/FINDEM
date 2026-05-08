package com.memoire.assistant.dto;

import java.util.UUID;

public class ApplyResponse {
    private UUID candidateId;
    private UUID applicationId;

    public ApplyResponse(UUID candidateId, UUID applicationId) {
        this.candidateId = candidateId;
        this.applicationId = applicationId;
    }

    public UUID getCandidateId() { return candidateId; }
    public UUID getApplicationId() { return applicationId; }
}
