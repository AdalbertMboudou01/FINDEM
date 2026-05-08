package com.memoire.assistant.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class JobStatsDTO {
    private UUID jobId;
    private String jobTitle;
    private String companyName;
    private int totalApplications;
    private long newApplications;
    private long pendingApplications;
    private long reviewedApplications;
    private long interviewApplications;
    private LocalDateTime createdAt;
    
    // Constructors
    public JobStatsDTO() {}
    
    // Getters & Setters
    public UUID getJobId() {
        return jobId;
    }
    
    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }
    
    public String getJobTitle() {
        return jobTitle;
    }
    
    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public int getTotalApplications() {
        return totalApplications;
    }
    
    public void setTotalApplications(int totalApplications) {
        this.totalApplications = totalApplications;
    }
    
    public long getNewApplications() {
        return newApplications;
    }
    
    public void setNewApplications(long newApplications) {
        this.newApplications = newApplications;
    }
    
    public long getPendingApplications() {
        return pendingApplications;
    }
    
    public void setPendingApplications(long pendingApplications) {
        this.pendingApplications = pendingApplications;
    }
    
    public long getReviewedApplications() {
        return reviewedApplications;
    }
    
    public void setReviewedApplications(long reviewedApplications) {
        this.reviewedApplications = reviewedApplications;
    }
    
    public long getInterviewApplications() {
        return interviewApplications;
    }
    
    public void setInterviewApplications(long interviewApplications) {
        this.interviewApplications = interviewApplications;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
