package com.memoire.assistant.dto;

import java.util.UUID;

public class CandidateSummaryDTO {
    private UUID candidateId;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean inPool;
    private String lastStatus;
    private int applicationsCount;
    // Ajoute d'autres champs utiles si besoin

    // Getters & Setters
    public UUID getCandidateId() { return candidateId; }
    public void setCandidateId(UUID candidateId) { this.candidateId = candidateId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Boolean getInPool() { return inPool; }
    public void setInPool(Boolean inPool) { this.inPool = inPool; }
    public String getLastStatus() { return lastStatus; }
    public void setLastStatus(String lastStatus) { this.lastStatus = lastStatus; }
    public int getApplicationsCount() { return applicationsCount; }
    public void setApplicationsCount(int applicationsCount) { this.applicationsCount = applicationsCount; }
}