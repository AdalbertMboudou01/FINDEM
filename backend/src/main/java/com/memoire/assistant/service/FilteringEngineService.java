package com.memoire.assistant.service;

import com.memoire.assistant.dto.ApplicationSummaryDTO;
import com.memoire.assistant.dto.ChatAnswerAnalysisDTO;
import com.memoire.assistant.model.Application;
import com.memoire.assistant.model.ApplicationSummary;
import com.memoire.assistant.model.Job;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.ApplicationSummaryRepository;
import com.memoire.assistant.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FilteringEngineService {

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationSummaryRepository applicationSummaryRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ChatAnswerService chatAnswerService;

    public List<ApplicationSummaryDTO> filterApplications(UUID jobId, FilteringCriteria criteria) {
        Job job = jobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("Offre non trouvée"));

        List<Application> applications = applicationRepository.findByJob_JobId(jobId);

        return applications.stream()
            .map(app -> {
                ApplicationSummary summary = applicationSummaryRepository
                    .findByApplication_ApplicationId(app.getApplicationId())
                    .orElse(null);
                if (summary == null) {
                    summary = generateSummaryForApplication(app.getApplicationId());
                }
                return applyFilters(summary, app, criteria);
            })
            .filter(summary -> summary != null)
            .collect(Collectors.toList());
    }

    private ApplicationSummary generateSummaryForApplication(UUID applicationId) {
        try {
            Application application = applicationRepository.findById(applicationId).orElse(null);
            if (application == null) return null;

            ApplicationSummary summary = new ApplicationSummary(application);
            ChatAnswerAnalysisDTO chatAnalysis = chatAnswerService.analyzeChatAnswers(applicationId);

            summary.setMotivationLevel(chatAnalysis.getMotivationLevel());
            summary.setTechnicalProfile(chatAnalysis.getTechnicalLevel());
            summary.setExperienceLevel("JUNIOR");
            summary.setRecommendedAction(chatAnalysis.getRecommendedAction());

            StringBuilder summaryText = new StringBuilder();
            summaryText.append("Analyse candidature (chatbot):\n\n");
            summaryText.append("Motivation: ").append(chatAnalysis.getMotivationLevel()).append("\n");
            summaryText.append("Niveau technique: ").append(chatAnalysis.getTechnicalLevel()).append("\n");
            summaryText.append("Complétude réponses: ")
                .append(String.format("%.1f%%", chatAnalysis.getCompletenessScore() * 100))
                .append("\n");

            if (chatAnalysis.getMissingInformation() != null && !chatAnalysis.getMissingInformation().isEmpty()) {
                summaryText.append("Informations manquantes: ")
                    .append(String.join(", ", chatAnalysis.getMissingInformation()))
                    .append("\n");
            }

            if (chatAnalysis.getInconsistencies() != null && !chatAnalysis.getInconsistencies().isEmpty()) {
                summaryText.append("Incohérences détectées: ")
                    .append(String.join(", ", chatAnalysis.getInconsistencies()))
                    .append("\n");
            }

            summary.setSummaryText(summaryText.toString());
            return applicationSummaryRepository.save(summary);
        } catch (Exception e) {
            Application application = applicationRepository.findById(applicationId).orElse(null);
            if (application == null) return null;

            ApplicationSummary summary = new ApplicationSummary(application);
            summary.setMotivationLevel("MEDIUM");
            summary.setTechnicalProfile("MEDIUM");
            summary.setExperienceLevel("JUNIOR");
            summary.setRecommendedAction("REVIEW");
            summary.setSummaryText("Synthèse générée automatiquement (mode fallback)");

            return applicationSummaryRepository.save(summary);
        }
    }

    private ApplicationSummaryDTO applyFilters(ApplicationSummary summary, Application application, FilteringCriteria criteria) {
        ApplicationSummaryDTO dto = convertToDTO(summary);
        if (dto == null) {
            return null;
        }

        if (criteria.getBlockingCriteria() != null && !criteria.getBlockingCriteria().isEmpty()) {
            for (String blockingCriterion : criteria.getBlockingCriteria()) {
                if (!passesBlockingCriterion(summary, application, blockingCriterion)) {
                    return null;
                }
            }
        }

        if (criteria.getMinMotivationLevel() != null &&
            !meetsMinimumLevel(summary.getMotivationLevel(), criteria.getMinMotivationLevel())) {
            return null;
        }

        if (criteria.getMinTechnicalLevel() != null &&
            !meetsMinimumLevel(summary.getTechnicalProfile(), criteria.getMinTechnicalLevel())) {
            return null;
        }

        if (criteria.getExperienceLevel() != null &&
            !criteria.getExperienceLevel().equals(summary.getExperienceLevel())) {
            return null;
        }

        if (criteria.getLocationMatch() != null &&
            !criteria.getLocationMatch().equals(summary.getLocationMatch())) {
            return null;
        }

        if (criteria.getRecommendedAction() != null &&
            !criteria.getRecommendedAction().equals(summary.getRecommendedAction())) {
            return null;
        }

        dto.setRelevanceScore(calculateRelevanceScore(summary, criteria));
        return dto;
    }

    private double calculateRelevanceScore(ApplicationSummary summary, FilteringCriteria criteria) {
        double score = 0.0;
        int criteriaCount = 0;

        if (criteria.getMinMotivationLevel() != null) {
            score += getMotivationScore(summary.getMotivationLevel()) * 0.4;
            criteriaCount++;
        }

        if (criteria.getMinTechnicalLevel() != null) {
            score += getTechnicalScore(summary.getTechnicalProfile()) * 0.4;
            criteriaCount++;
        }

        if (criteria.getExperienceLevel() != null) {
            score += getExperienceScore(summary.getExperienceLevel()) * 0.2;
            criteriaCount++;
        }

        return criteriaCount > 0 ? score / criteriaCount : 0.0;
    }

    private boolean passesBlockingCriterion(ApplicationSummary summary, Application application, String criterion) {
        switch (criterion) {
            case "DISPONIBILITY":
                return summary.getAvailabilityStatus() != null &&
                       !"NON_SPECIFIE".equals(summary.getAvailabilityStatus());
            case "CV_REQUIRED":
                return application.getCandidate() != null && application.getCandidate().getCvPath() != null;
            case "LOCATION":
                return "PERFECT".equals(summary.getLocationMatch()) ||
                       "REMOTE_COMPATIBLE".equals(summary.getLocationMatch());
            case "CONSENT":
                return application.getCandidate().getConsent() != null && application.getCandidate().getConsent();
            case "COMPLETE_ANSWERS":
                return summary.getMotivationLevel() != null && summary.getTechnicalProfile() != null;
            default:
                return true;
        }
    }

    private boolean meetsMinimumLevel(String currentLevel, String minLevel) {
        if (currentLevel == null || minLevel == null) return false;
        return getLevelValue(currentLevel) >= getLevelValue(minLevel);
    }

    private int getLevelValue(String level) {
        switch (level) {
            case "HIGH":
            case "STRONG":
                return 3;
            case "MEDIUM":
            case "INTERMEDIATE":
                return 2;
            case "LOW":
            case "WEAK":
            case "JUNIOR":
                return 1;
            default:
                return 0;
        }
    }

    private double getMotivationScore(String level) {
        switch (level) {
            case "HIGH":
                return 1.0;
            case "MEDIUM":
                return 0.7;
            case "LOW":
                return 0.3;
            default:
                return 0.0;
        }
    }

    private double getTechnicalScore(String level) {
        switch (level) {
            case "STRONG":
                return 1.0;
            case "MEDIUM":
                return 0.7;
            case "WEAK":
                return 0.3;
            default:
                return 0.0;
        }
    }

    private double getExperienceScore(String level) {
        switch (level) {
            case "SENIOR":
                return 1.0;
            case "INTERMEDIATE":
                return 0.7;
            case "JUNIOR":
                return 0.4;
            default:
                return 0.0;
        }
    }

    private ApplicationSummaryDTO convertToDTO(ApplicationSummary summary) {
        if (summary == null) return null;

        ApplicationSummaryDTO dto = new ApplicationSummaryDTO();
        dto.setSummaryId(summary.getSummaryId());
        dto.setApplicationId(summary.getApplication().getApplicationId());
        dto.setMotivationLevel(summary.getMotivationLevel());
        dto.setTechnicalProfile(summary.getTechnicalProfile());
        dto.setExperienceLevel(summary.getExperienceLevel());
        dto.setKeySkills(summary.getKeySkills());
        dto.setProjectHighlights(summary.getProjectHighlights());
        dto.setAvailabilityStatus(summary.getAvailabilityStatus());
        dto.setLocationMatch(summary.getLocationMatch());
        dto.setRecommendedAction(summary.getRecommendedAction());
        dto.setSummaryText(summary.getSummaryText());
        dto.setBlockingCriteria(summary.getBlockingCriteria());
        dto.setPositivePoints(summary.getPositivePoints());
        dto.setConcerns(summary.getConcerns());
        dto.setGeneratedAt(summary.getGeneratedAt().toString());
        dto.setGeneratedBy(summary.getGeneratedBy());
        return dto;
    }

    public static class FilteringCriteria {
        private List<String> blockingCriteria;
        private String minMotivationLevel;
        private String minTechnicalLevel;
        private String experienceLevel;
        private String locationMatch;
        private String recommendedAction;

        private Double minCVScore;
        private List<String> requiredSkills;
        private Integer minExperienceYears;
        private Double minJobMatch;

        public FilteringCriteria() {}

        public List<String> getBlockingCriteria() { return blockingCriteria; }
        public void setBlockingCriteria(List<String> blockingCriteria) { this.blockingCriteria = blockingCriteria; }
        public String getMinMotivationLevel() { return minMotivationLevel; }
        public void setMinMotivationLevel(String minMotivationLevel) { this.minMotivationLevel = minMotivationLevel; }
        public String getMinTechnicalLevel() { return minTechnicalLevel; }
        public void setMinTechnicalLevel(String minTechnicalLevel) { this.minTechnicalLevel = minTechnicalLevel; }
        public String getExperienceLevel() { return experienceLevel; }
        public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }
        public String getLocationMatch() { return locationMatch; }
        public void setLocationMatch(String locationMatch) { this.locationMatch = locationMatch; }
        public String getRecommendedAction() { return recommendedAction; }
        public void setRecommendedAction(String recommendedAction) { this.recommendedAction = recommendedAction; }
        public Double getMinCVScore() { return minCVScore; }
        public void setMinCVScore(Double minCVScore) { this.minCVScore = minCVScore; }
        public List<String> getRequiredSkills() { return requiredSkills; }
        public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }
        public Integer getMinExperienceYears() { return minExperienceYears; }
        public void setMinExperienceYears(Integer minExperienceYears) { this.minExperienceYears = minExperienceYears; }
        public Double getMinJobMatch() { return minJobMatch; }
        public void setMinJobMatch(Double minJobMatch) { this.minJobMatch = minJobMatch; }
    }
}
