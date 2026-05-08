package com.memoire.assistant.dto;

import java.util.UUID;

public class ChatStatsDTO {
    private UUID jobId;
    private int totalSessions;
    private int completedSessions;
    private int abandonedSessions;
    private double averageCompletionScore;
    private double averageDuration;
    
    // Constructors
    public ChatStatsDTO() {}
    
    // Getters & Setters
    public UUID getJobId() {
        return jobId;
    }
    
    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }
    
    public int getTotalSessions() {
        return totalSessions;
    }
    
    public void setTotalSessions(int totalSessions) {
        this.totalSessions = totalSessions;
    }
    
    public int getCompletedSessions() {
        return completedSessions;
    }
    
    public void setCompletedSessions(int completedSessions) {
        this.completedSessions = completedSessions;
    }
    
    public int getAbandonedSessions() {
        return abandonedSessions;
    }
    
    public void setAbandonedSessions(int abandonedSessions) {
        this.abandonedSessions = abandonedSessions;
    }
    
    public double getAverageCompletionScore() {
        return averageCompletionScore;
    }
    
    public void setAverageCompletionScore(double averageCompletionScore) {
        this.averageCompletionScore = averageCompletionScore;
    }
    
    public double getAverageDuration() {
        return averageDuration;
    }
    
    public void setAverageDuration(double averageDuration) {
        this.averageDuration = averageDuration;
    }
}
