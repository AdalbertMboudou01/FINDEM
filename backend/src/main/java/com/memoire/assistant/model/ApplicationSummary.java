package com.memoire.assistant.model;

import jakarta.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "application_summaries")
public class ApplicationSummary {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID summaryId;

    @OneToOne
    @JoinColumn(name = "application_id")
    private Application application;

    private String motivationLevel; // HIGH, MEDIUM, LOW
    private String technicalProfile; // STRONG, MEDIUM, WEAK
    private String experienceLevel; // JUNIOR, INTERMEDIATE, SENIOR
    private String keySkills;
    private String projectHighlights;
    private String availabilityStatus;
    private String locationMatch;
    private String recommendedAction; // INTERVIEW, REVIEW, REJECT, POOL
    private String summaryText;
    private String blockingCriteria;
    private String positivePoints;
    private String concerns;
    private Date generatedAt;
    private String generatedBy; // SYSTEM, USER

    // Constructors
    public ApplicationSummary() {}

    public ApplicationSummary(Application application) {
        this.application = application;
        this.generatedAt = new Date();
        this.generatedBy = "SYSTEM";
    }

    // Getters & Setters
    public UUID getSummaryId() {
        return summaryId;
    }

    public void setSummaryId(UUID summaryId) {
        this.summaryId = summaryId;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
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

    public Date getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Date generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }
}
