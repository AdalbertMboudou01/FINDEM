package com.memoire.assistant.dto;

import java.util.UUID;

public class AuthResponse {
    private String token;
    private String role;
    private UUID userId;
    private UUID recruiterId;
    private UUID companyId;
    private boolean onboardingCompleted;

    public AuthResponse(String token, String role) {
        this.token = token;
        this.role = role;
    }

    public AuthResponse(
        String token,
        String role,
        UUID userId,
        UUID recruiterId,
        UUID companyId,
        boolean onboardingCompleted
    ) {
        this.token = token;
        this.role = role;
        this.userId = userId;
        this.recruiterId = recruiterId;
        this.companyId = companyId;
        this.onboardingCompleted = onboardingCompleted;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getRecruiterId() { return recruiterId; }
    public void setRecruiterId(UUID recruiterId) { this.recruiterId = recruiterId; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public boolean isOnboardingCompleted() { return onboardingCompleted; }
    public void setOnboardingCompleted(boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; }
}
