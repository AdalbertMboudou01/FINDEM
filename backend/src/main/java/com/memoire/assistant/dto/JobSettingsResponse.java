package com.memoire.assistant.dto;

import java.util.UUID;

public class JobSettingsResponse {
    private UUID jobId;
    private Integer maxCandidatures;
    private boolean autoClose;

    public JobSettingsResponse(UUID jobId, Integer maxCandidatures, boolean autoClose) {
        this.jobId = jobId;
        this.maxCandidatures = maxCandidatures;
        this.autoClose = autoClose;
    }

    public UUID getJobId() {
        return jobId;
    }

    public Integer getMaxCandidatures() {
        return maxCandidatures;
    }

    public boolean isAutoClose() {
        return autoClose;
    }
}
