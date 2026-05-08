package com.memoire.assistant.dto;

public class JobSettingsRequest {
    private Integer maxCandidatures;
    private boolean autoClose = true;

    public Integer getMaxCandidatures() {
        return maxCandidatures;
    }

    public void setMaxCandidatures(Integer maxCandidatures) {
        this.maxCandidatures = maxCandidatures;
    }

    public boolean isAutoClose() {
        return autoClose;
    }

    public void setAutoClose(boolean autoClose) {
        this.autoClose = autoClose;
    }
}
