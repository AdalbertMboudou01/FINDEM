package com.memoire.assistant.dto;

import java.util.List;
import java.util.UUID;

public class ApplicationSummaryDTO {
    private UUID summaryId;
    private UUID applicationId;
    private String motivationLevel;
    private String technicalProfile;
    private String experienceLevel;
    private String keySkills;
    private String projectHighlights;
    private String availabilityStatus;
    private String locationMatch;
    private String recommendedAction;
    private String summaryText;
    private String blockingCriteria;
    private String positivePoints;
    private String concerns;
    private String generatedAt;
    private String generatedBy;
    
    // Champs ajoutés pour l'intégration CV
    private Double relevanceScore;
    private Double cvScore;
    private Double cvTechnicalScore;
    private Double cvExperienceScore;
    private Double jobMatch;
    private List<String> technicalSkills;
    private Integer experienceYears;

    // Constructors
    public ApplicationSummaryDTO() {}

    // Getters & Setters
    public UUID getSummaryId() {
        return summaryId;
    }

    public void setSummaryId(UUID summaryId) {
        this.summaryId = summaryId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public String getMotivationLevel() {
        return motivationLevel;
    }

    public void setMotivationLevel(String motivationLevel) {
        this.motivationLevel = motivationLevel;
    }

    public String getTechnicalProfile() {
        return technicalProfile;
    }

    public void setTechnicalProfile(String technicalProfile) {
        this.technicalProfile = technicalProfile;
    }

    public String getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(String experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public String getKeySkills() {
        return keySkills;
    }

    public void setKeySkills(String keySkills) {
        this.keySkills = keySkills;
    }

    public String getProjectHighlights() {
        return projectHighlights;
    }

    public void setProjectHighlights(String projectHighlights) {
        this.projectHighlights = projectHighlights;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public String getLocationMatch() {
        return locationMatch;
    }

    public void setLocationMatch(String locationMatch) {
        this.locationMatch = locationMatch;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public String getBlockingCriteria() {
        return blockingCriteria;
    }

    public void setBlockingCriteria(String blockingCriteria) {
        this.blockingCriteria = blockingCriteria;
    }

    public String getPositivePoints() {
        return positivePoints;
    }

    public void setPositivePoints(String positivePoints) {
        this.positivePoints = positivePoints;
    }

    public String getConcerns() {
        return concerns;
    }

    public void setConcerns(String concerns) {
        this.concerns = concerns;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }
    
    // Getters & Setters pour les champs CV
    public Double getRelevanceScore() {
        return relevanceScore;
    }
    
    public void setRelevanceScore(Double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }
    
    public Double getCvScore() {
        return cvScore;
    }
    
    public void setCvScore(Double cvScore) {
        this.cvScore = cvScore;
    }
    
    public Double getCvTechnicalScore() {
        return cvTechnicalScore;
    }
    
    public void setCvTechnicalScore(Double cvTechnicalScore) {
        this.cvTechnicalScore = cvTechnicalScore;
    }
    
    public Double getCvExperienceScore() {
        return cvExperienceScore;
    }
    
    public void setCvExperienceScore(Double cvExperienceScore) {
        this.cvExperienceScore = cvExperienceScore;
    }
    
    public Double getJobMatch() {
        return jobMatch;
    }
    
    public void setJobMatch(Double jobMatch) {
        this.jobMatch = jobMatch;
    }
    
    public List<String> getTechnicalSkills() {
        return technicalSkills;
    }
    
    public void setTechnicalSkills(List<String> technicalSkills) {
        this.technicalSkills = technicalSkills;
    }
    
    public Integer getExperienceYears() {
        return experienceYears;
    }
    
    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }
}
