package com.memoire.assistant.dto;

import java.util.List;

public class FilteringCriteria {
    
    private String minMotivationLevel;
    private String minTechnicalLevel;
    private String experienceLevel;
    private String locationMatch;
    private String recommendedAction;
    private List<String> blockingCriteria;
    
    // Nouveaux critères basés sur les données CV
    private Double minCVScore;
    private List<String> requiredSkills;
    private Integer minExperienceYears;
    private Double minJobMatch;
    
    // Constructeurs
    public FilteringCriteria() {}
    
    public FilteringCriteria(String minMotivationLevel, String minTechnicalLevel) {
        this.minMotivationLevel = minMotivationLevel;
        this.minTechnicalLevel = minTechnicalLevel;
    }
    
    // Getters & Setters
    public String getMinMotivationLevel() {
        return minMotivationLevel;
    }
    
    public void setMinMotivationLevel(String minMotivationLevel) {
        this.minMotivationLevel = minMotivationLevel;
    }
    
    public String getMinTechnicalLevel() {
        return minTechnicalLevel;
    }
    
    public void setMinTechnicalLevel(String minTechnicalLevel) {
        this.minTechnicalLevel = minTechnicalLevel;
    }
    
    public String getExperienceLevel() {
        return experienceLevel;
    }
    
    public void setExperienceLevel(String experienceLevel) {
        this.experienceLevel = experienceLevel;
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
    
    public List<String> getBlockingCriteria() {
        return blockingCriteria;
    }
    
    public void setBlockingCriteria(List<String> blockingCriteria) {
        this.blockingCriteria = blockingCriteria;
    }
    
    public Double getMinCVScore() {
        return minCVScore;
    }
    
    public void setMinCVScore(Double minCVScore) {
        this.minCVScore = minCVScore;
    }
    
    public List<String> getRequiredSkills() {
        return requiredSkills;
    }
    
    public void setRequiredSkills(List<String> requiredSkills) {
        this.requiredSkills = requiredSkills;
    }
    
    public Integer getMinExperienceYears() {
        return minExperienceYears;
    }
    
    public void setMinExperienceYears(Integer minExperienceYears) {
        this.minExperienceYears = minExperienceYears;
    }
    
    public Double getMinJobMatch() {
        return minJobMatch;
    }
    
    public void setMinJobMatch(Double minJobMatch) {
        this.minJobMatch = minJobMatch;
    }
    
    // Méthodes utilitaires
    public static FilteringCriteria forTechnicalLevel(String minLevel) {
        FilteringCriteria criteria = new FilteringCriteria();
        criteria.setMinTechnicalLevel(minLevel);
        return criteria;
    }
    
    public static FilteringCriteria forMotivationLevel(String minLevel) {
        FilteringCriteria criteria = new FilteringCriteria();
        criteria.setMinMotivationLevel(minLevel);
        return criteria;
    }
    
    public static FilteringCriteria forExperience(String experienceLevel) {
        FilteringCriteria criteria = new FilteringCriteria();
        criteria.setExperienceLevel(experienceLevel);
        return criteria;
    }
    
    public static FilteringCriteria withCVRequirements(Double minCVScore, List<String> requiredSkills) {
        FilteringCriteria criteria = new FilteringCriteria();
        criteria.setMinCVScore(minCVScore);
        criteria.setRequiredSkills(requiredSkills);
        return criteria;
    }
    
    @Override
    public String toString() {
        return "FilteringCriteria{" +
                "minMotivationLevel='" + minMotivationLevel + '\'' +
                ", minTechnicalLevel='" + minTechnicalLevel + '\'' +
                ", experienceLevel='" + experienceLevel + '\'' +
                ", locationMatch='" + locationMatch + '\'' +
                ", recommendedAction='" + recommendedAction + '\'' +
                ", blockingCriteria=" + blockingCriteria +
                ", minCVScore=" + minCVScore +
                ", requiredSkills=" + requiredSkills +
                ", minExperienceYears=" + minExperienceYears +
                ", minJobMatch=" + minJobMatch +
                '}';
    }
}
