package com.memoire.assistant.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class CompanyStatsDTO {
    private UUID companyId;
    private String companyName;
    private int recruiterCount;
    private int activeJobCount;
    private int totalJobCount;
    private LocalDateTime createdAt;
    
    // Constructors
    public CompanyStatsDTO() {}
    
    // Getters & Setters
    public UUID getCompanyId() {
        return companyId;
    }
    
    public void setCompanyId(UUID companyId) {
        this.companyId = companyId;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public int getRecruiterCount() {
        return recruiterCount;
    }
    
    public void setRecruiterCount(int recruiterCount) {
        this.recruiterCount = recruiterCount;
    }
    
    public int getActiveJobCount() {
        return activeJobCount;
    }
    
    public void setActiveJobCount(int activeJobCount) {
        this.activeJobCount = activeJobCount;
    }
    
    public int getTotalJobCount() {
        return totalJobCount;
    }
    
    public void setTotalJobCount(int totalJobCount) {
        this.totalJobCount = totalJobCount;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
