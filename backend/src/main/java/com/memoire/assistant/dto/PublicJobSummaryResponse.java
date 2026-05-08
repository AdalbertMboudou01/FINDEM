package com.memoire.assistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class PublicJobSummaryResponse {
    private UUID jobId;
    private String title;
    @JsonProperty("isAccepting")
    private boolean isAccepting;
    private int candidateCount;
    private Integer maxCandidatures;

    public PublicJobSummaryResponse(UUID jobId, String title, boolean isAccepting, int candidateCount, Integer maxCandidatures) {
        this.jobId = jobId;
        this.title = title;
        this.isAccepting = isAccepting;
        this.candidateCount = candidateCount;
        this.maxCandidatures = maxCandidatures;
    }

    public UUID getJobId() {
        return jobId;
    }

    public String getTitle() {
        return title;
    }

    @JsonProperty("isAccepting")
    public boolean isAccepting() {
        return isAccepting;
    }

    public int getCandidateCount() {
        return candidateCount;
    }

    public Integer getMaxCandidatures() {
        return maxCandidatures;
    }
}
