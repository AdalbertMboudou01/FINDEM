package com.memoire.assistant.service;

import com.memoire.assistant.dto.AnalysisFactFeedbackRequest;
import com.memoire.assistant.dto.AnalysisFactFeedbackResponse;
import com.memoire.assistant.dto.AnalysisQualityMetricsDTO;
import com.memoire.assistant.model.AnalysisFactFeedback;
import com.memoire.assistant.model.Application;
import com.memoire.assistant.repository.AnalysisFactFeedbackRepository;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AnalysisFactFeedbackService {

    @Autowired
    private AnalysisFactFeedbackRepository feedbackRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    public AnalysisFactFeedbackResponse saveFeedback(UUID applicationId, AnalysisFactFeedbackRequest request) {
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new RuntimeException("Candidature non trouvée"));

        String decision = normalizeDecision(request.getDecision());
        if ("CORRECTED".equals(decision)) {
            String correctedFinding = safe(request.getCorrectedFinding());
            if (correctedFinding.isBlank()) {
                throw new RuntimeException("correctedFinding est obligatoire quand decision=CORRECTED");
            }
        }
        if ("REJECTED".equals(decision) && safe(request.getReviewerComment()).isBlank()) {
            throw new RuntimeException("reviewerComment est obligatoire quand decision=REJECTED");
        }

        AnalysisFactFeedback feedback = new AnalysisFactFeedback();
        feedback.setApplication(application);
        feedback.setDimension(safeOrDefault(request.getDimension(), "general"));
        feedback.setFinding(safe(request.getFinding()));
        feedback.setEvidence(safe(request.getEvidence()));
        feedback.setDecision(decision);
        feedback.setCorrectedFinding(safe(request.getCorrectedFinding()));
        feedback.setReviewerComment(safe(request.getReviewerComment()));

        if (feedback.getFinding().isBlank()) {
            throw new RuntimeException("finding est obligatoire");
        }

        AnalysisFactFeedback saved = feedbackRepository.save(feedback);
        return toResponse(saved);
    }

    public List<AnalysisFactFeedbackResponse> getFeedbackByApplication(UUID applicationId) {
        return feedbackRepository.findByApplication_ApplicationIdOrderByCreatedAtDesc(applicationId)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    public List<AnalysisFactFeedbackResponse> getLatestFeedbackByApplication(UUID applicationId) {
        List<AnalysisFactFeedbackResponse> ordered = getFeedbackByApplication(applicationId);
        Map<String, AnalysisFactFeedbackResponse> latestByKey = new LinkedHashMap<>();

        for (AnalysisFactFeedbackResponse item : ordered) {
            String key = (item.getDimension() == null ? "general" : item.getDimension())
                + "::"
                + (item.getFinding() == null ? "" : item.getFinding());

            if (!latestByKey.containsKey(key)) {
                latestByKey.put(key, item);
            }
        }

        return latestByKey.values().stream().collect(Collectors.toList());
    }

    public AnalysisQualityMetricsDTO getQualityMetrics(int days) {
        int safeDays = Math.max(1, Math.min(days, 365));
        LocalDateTime windowStart = LocalDateTime.now().minusDays(safeDays);
        UUID companyId = TenantContext.getCompanyId();

        List<AnalysisFactFeedback> scopedFeedback = companyId == null
            ? feedbackRepository.findByCreatedAtAfterOrderByCreatedAtDesc(windowStart)
            : feedbackRepository.findByApplication_Job_Company_CompanyIdAndCreatedAtAfterOrderByCreatedAtDesc(companyId, windowStart);

        Map<String, AnalysisFactFeedback> latestByFact = new LinkedHashMap<>();
        for (AnalysisFactFeedback item : scopedFeedback) {
            String key = buildFactKey(item);
            latestByFact.putIfAbsent(key, item);
        }

        List<AnalysisFactFeedback> latestFacts = new ArrayList<>(latestByFact.values());
        Set<UUID> reviewedApplications = latestFacts.stream()
            .map(item -> item.getApplication() == null ? null : item.getApplication().getApplicationId())
            .filter(v -> v != null)
            .collect(Collectors.toCollection(HashSet::new));

        int confirmed = countDecision(latestFacts, "CONFIRMED");
        int corrected = countDecision(latestFacts, "CORRECTED");
        int rejected = countDecision(latestFacts, "REJECTED");
        int reviewedFacts = latestFacts.size();

        AnalysisQualityMetricsDTO dto = new AnalysisQualityMetricsDTO();
        dto.setWindowDays(safeDays);
        dto.setGeneratedAt(LocalDateTime.now());
        dto.setFeedbackEvents(scopedFeedback.size());
        dto.setReviewedFacts(reviewedFacts);
        dto.setReviewedApplications(reviewedApplications.size());
        dto.setConfirmedFacts(confirmed);
        dto.setCorrectedFacts(corrected);
        dto.setRejectedFacts(rejected);
        dto.setPrecisionScore(reviewedFacts == 0 ? 0.0 : ((double) confirmed + 0.5 * (double) corrected) / (double) reviewedFacts);
        dto.setCorrectionRate(reviewedFacts == 0 ? 0.0 : (double) corrected / (double) reviewedFacts);
        dto.setRejectionRate(reviewedFacts == 0 ? 0.0 : (double) rejected / (double) reviewedFacts);
        dto.setByDimension(buildByDimension(latestFacts));

        return dto;
    }

    private List<AnalysisQualityMetricsDTO.DimensionMetricsDTO> buildByDimension(List<AnalysisFactFeedback> latestFacts) {
        Map<String, List<AnalysisFactFeedback>> grouped = new HashMap<>();
        for (AnalysisFactFeedback item : latestFacts) {
            String dimension = safeOrDefault(item.getDimension(), "general").toLowerCase(Locale.ROOT);
            grouped.computeIfAbsent(dimension, ignored -> new ArrayList<>()).add(item);
        }

        return grouped.entrySet().stream()
            .map(entry -> {
                List<AnalysisFactFeedback> facts = entry.getValue();
                int total = facts.size();
                int confirmed = countDecision(facts, "CONFIRMED");
                int corrected = countDecision(facts, "CORRECTED");
                int rejected = countDecision(facts, "REJECTED");

                AnalysisQualityMetricsDTO.DimensionMetricsDTO item = new AnalysisQualityMetricsDTO.DimensionMetricsDTO();
                item.setDimension(entry.getKey());
                item.setReviewedFacts(total);
                item.setConfirmedFacts(confirmed);
                item.setCorrectedFacts(corrected);
                item.setRejectedFacts(rejected);
                item.setPrecisionScore(total == 0 ? 0.0 : ((double) confirmed + 0.5 * (double) corrected) / (double) total);
                return item;
            })
            .sorted(Comparator.comparing(AnalysisQualityMetricsDTO.DimensionMetricsDTO::getReviewedFacts).reversed())
            .collect(Collectors.toList());
    }

    private int countDecision(List<AnalysisFactFeedback> items, String decision) {
        return (int) items.stream()
            .filter(item -> decision.equalsIgnoreCase(safe(item.getDecision())))
            .count();
    }

    private String buildFactKey(AnalysisFactFeedback item) {
        String applicationId = item.getApplication() == null || item.getApplication().getApplicationId() == null
            ? ""
            : item.getApplication().getApplicationId().toString();
        return applicationId
            + "::"
            + safeOrDefault(item.getDimension(), "general")
            + "::"
            + safe(item.getFinding());
    }

    private AnalysisFactFeedbackResponse toResponse(AnalysisFactFeedback feedback) {
        AnalysisFactFeedbackResponse response = new AnalysisFactFeedbackResponse();
        response.setFeedbackId(feedback.getFeedbackId().toString());
        response.setApplicationId(feedback.getApplication().getApplicationId().toString());
        response.setDimension(feedback.getDimension());
        response.setFinding(feedback.getFinding());
        response.setEvidence(feedback.getEvidence());
        response.setDecision(feedback.getDecision());
        response.setCorrectedFinding(feedback.getCorrectedFinding());
        response.setReviewerComment(feedback.getReviewerComment());
        response.setCreatedAt(feedback.getCreatedAt());
        return response;
    }

    private String normalizeDecision(String value) {
        String normalized = safe(value).toUpperCase(Locale.ROOT);
        if (!normalized.equals("CONFIRMED") && !normalized.equals("CORRECTED") && !normalized.equals("REJECTED")) {
            throw new RuntimeException("decision doit valoir CONFIRMED, CORRECTED ou REJECTED");
        }
        return normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeOrDefault(String value, String fallback) {
        String normalized = safe(value);
        return normalized.isBlank() ? fallback : normalized;
    }
}
