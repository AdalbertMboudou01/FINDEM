package com.memoire.assistant.service;

import com.memoire.assistant.dto.DecisionDTO;
import com.memoire.assistant.dto.DecisionInputDTO;
import com.memoire.assistant.model.ApplicationActivity.EventType;
import com.memoire.assistant.model.Decision;
import com.memoire.assistant.model.DecisionInput;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.DecisionInputRepository;
import com.memoire.assistant.repository.DecisionRepository;
import com.memoire.assistant.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DecisionService {

    @Autowired private DecisionInputRepository decisionInputRepository;
    @Autowired private DecisionRepository decisionRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private ApplicationActivityService activityService;
    @Autowired private AIDecisionReviewService aiDecisionReviewService;

    public List<DecisionInputDTO> getInputs(UUID applicationId) {
        UUID companyId = TenantContext.getCompanyId();
        return decisionInputRepository
                .findByApplicationIdAndCompanyIdOrderByCreatedAtAsc(applicationId, companyId)
                .stream().map(this::toInputDTO).collect(Collectors.toList());
    }

    public DecisionInputDTO addInput(UUID applicationId, String sentiment, String comment, Integer confidence) {
        UUID companyId = TenantContext.getCompanyId();
        UUID actorId = TenantContext.getRecruiterId() != null
                ? TenantContext.getRecruiterId()
                : TenantContext.getUserId();

        applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application non trouvée"));

        DecisionInput input = new DecisionInput();
        input.setApplicationId(applicationId);
        input.setCompanyId(companyId);
        input.setAuthorId(actorId);
        input.setSentiment(DecisionInput.Sentiment.valueOf(sentiment));
        input.setComment(comment);
        input.setConfidence(confidence);

        DecisionInput saved = decisionInputRepository.save(input);

        activityService.logEvent(applicationId, EventType.DECISION_RECORDED, Map.of(
                "type", "INPUT",
                "sentiment", sentiment,
                "authorId", actorId.toString()
        ));

        return toInputDTO(saved);
    }

    public DecisionDTO getDecision(UUID applicationId) {
        UUID companyId = TenantContext.getCompanyId();
        List<DecisionInputDTO> inputs = getInputs(applicationId);
        DecisionDTO dto = new DecisionDTO();
        dto.setApplicationId(applicationId);
        dto.setInputs(inputs);

        decisionRepository.findByApplicationIdAndCompanyId(applicationId, companyId)
                .ifPresent(d -> {
                    dto.setId(d.getId());
                    dto.setFinalStatus(d.getFinalStatus());
                    dto.setRationale(d.getRationale());
                    dto.setDecidedBy(d.getDecidedBy());
                    dto.setDecidedAt(d.getDecidedAt());
                    dto.setAiReview(d.getAiReview());
                });

        if (dto.getId() == null && inputs.isEmpty()) {
            dto.setBlockingReason("Aucun avis enregistré pour cette candidature.");
        }

        return dto;
    }

    public DecisionDTO recordFinalDecision(UUID applicationId, String finalStatus, String rationale) {
        UUID companyId = TenantContext.getCompanyId();
        UUID actorId = TenantContext.getRecruiterId() != null
                ? TenantContext.getRecruiterId()
                : TenantContext.getUserId();

        Decision decision = decisionRepository
                .findByApplicationIdAndCompanyId(applicationId, companyId)
                .orElse(new Decision());

        decision.setApplicationId(applicationId);
        decision.setCompanyId(companyId);
        decision.setFinalStatus(finalStatus);
        decision.setRationale(rationale);
        decision.setDecidedBy(actorId);
        decision.setDecidedAt(LocalDateTime.now());

        String aiReview = aiDecisionReviewService.generateReview(applicationId, companyId, finalStatus, rationale);
        decision.setAiReview(aiReview);

        Decision saved = decisionRepository.save(decision);

        activityService.logEvent(applicationId, EventType.DECISION_RECORDED, Map.of(
                "type", "FINAL",
                "finalStatus", finalStatus,
                "decidedBy", actorId.toString()
        ));

        DecisionDTO dto = new DecisionDTO();
        dto.setId(saved.getId());
        dto.setApplicationId(saved.getApplicationId());
        dto.setFinalStatus(saved.getFinalStatus());
        dto.setRationale(saved.getRationale());
        dto.setDecidedBy(saved.getDecidedBy());
        dto.setDecidedAt(saved.getDecidedAt());
        dto.setAiReview(saved.getAiReview());
        dto.setInputs(getInputs(applicationId));
        return dto;
    }

    private DecisionInputDTO toInputDTO(DecisionInput d) {
        DecisionInputDTO dto = new DecisionInputDTO();
        dto.setId(d.getId());
        dto.setApplicationId(d.getApplicationId());
        dto.setAuthorId(d.getAuthorId());
        dto.setSentiment(d.getSentiment().name());
        dto.setComment(d.getComment());
        dto.setConfidence(d.getConfidence());
        dto.setCreatedAt(d.getCreatedAt());
        return dto;
    }
}
