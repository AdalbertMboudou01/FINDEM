package com.memoire.assistant.service;

import com.memoire.assistant.dto.DecisionDTO;
import com.memoire.assistant.dto.DecisionInputDTO;
import com.memoire.assistant.model.Application;
import com.memoire.assistant.model.ApplicationActivity.EventType;
import com.memoire.assistant.model.Decision;
import com.memoire.assistant.model.DecisionInput;
import com.memoire.assistant.model.InternalNotification;
import com.memoire.assistant.model.Recruiter;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.ApplicationStatusRepository;
import com.memoire.assistant.repository.DecisionInputRepository;
import com.memoire.assistant.repository.DecisionRepository;
import com.memoire.assistant.repository.RecruiterRepository;
import com.memoire.assistant.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DecisionService {

    @Autowired private DecisionInputRepository decisionInputRepository;
    @Autowired private DecisionRepository decisionRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private ApplicationStatusRepository statusRepository;
    @Autowired private RecruiterRepository recruiterRepository;
    @Autowired private ApplicationActivityService activityService;
    @Autowired private AIDecisionReviewService aiDecisionReviewService;
    @Autowired private InternalNotificationService notificationService;

    public List<DecisionInputDTO> getInputs(UUID applicationId) {
        UUID companyId = TenantContext.getCompanyId();
        UUID actorId = TenantContext.getActorId();

        List<DecisionInput> inputs = decisionInputRepository
                .findByApplicationIdAndCompanyIdOrderByCreatedAtAsc(applicationId, companyId);

        Set<UUID> authorIds = inputs.stream().map(DecisionInput::getAuthorId).collect(Collectors.toSet());
        Map<UUID, String> nameByAuthorId = recruiterRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(Recruiter::getRecruiterId,
                    r -> r.getName() != null ? r.getName() : r.getEmail()));

        return inputs.stream()
                .map(d -> toInputDTO(d, nameByAuthorId.getOrDefault(d.getAuthorId(), "Membre de l'équipe"), actorId))
                .collect(Collectors.toList());
    }

    public DecisionInputDTO updateInput(UUID applicationId, UUID inputId, String sentiment, String comment, Integer confidence) {
        UUID actorId = TenantContext.getActorId();

        DecisionInput input = decisionInputRepository.findById(inputId)
                .orElseThrow(() -> new RuntimeException("Avis introuvable"));

        if (!input.getAuthorId().equals(actorId)) {
            throw new IllegalStateException("Vous ne pouvez modifier que votre propre avis.");
        }

        input.setSentiment(DecisionInput.Sentiment.valueOf(sentiment));
        input.setComment(comment);
        input.setConfidence(confidence);

        DecisionInput saved = decisionInputRepository.save(input);

        String authorName = recruiterRepository.findById(actorId)
                .map(r -> r.getName() != null ? r.getName() : r.getEmail())
                .orElse("Membre de l'équipe");
        return toInputDTO(saved, authorName, actorId);
    }

    public DecisionInputDTO addInput(UUID applicationId, String sentiment, String comment, Integer confidence) {
        UUID companyId = TenantContext.getCompanyId();
        UUID actorId = TenantContext.getActorId();

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application non trouvée"));

        if (decisionInputRepository.existsByApplicationIdAndAuthorIdAndCompanyId(applicationId, actorId, companyId)) {
            throw new IllegalStateException("Vous avez déjà soumis un avis pour cette candidature.");
        }

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

        String authorName = recruiterRepository.findById(actorId)
                .map(r -> r.getName() != null ? r.getName() : r.getEmail())
                .orElse("Membre de l'équipe");

        try {
            Recruiter ownerRecruiter = application.getJob().getOwnerRecruiter();
            if (ownerRecruiter != null && !ownerRecruiter.getRecruiterId().equals(actorId)) {
                String candidateName = buildCandidateName(application);
                notificationService.notify(
                    ownerRecruiter.getRecruiterId(), companyId,
                    InternalNotification.Type.OPINION_SUBMITTED,
                    "Nouvel avis sur une candidature",
                    authorName + " a donné son avis (" + sentiment.toLowerCase() + ") sur " + candidateName,
                    "APPLICATION", applicationId
                );
            }
        } catch (Exception ignored) {}

        return toInputDTO(saved, authorName, actorId);
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

    /** Seul le MANAGER peut enregistrer la décision finale. */
    public DecisionDTO recordFinalDecision(UUID applicationId, String finalStatus, String rationale) {
        UUID companyId = TenantContext.getCompanyId();
        UUID actorId = TenantContext.getActorId();

        Recruiter actor = recruiterRepository.findById(actorId)
                .orElseThrow(() -> new IllegalStateException("Recruteur introuvable"));
        String actorRole = actor.getRole() != null ? actor.getRole().toUpperCase() : "";
        if (!"MANAGER".equals(actorRole)) {
            throw new IllegalStateException("Seul le manager peut enregistrer la décision finale.");
        }

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

        // Mettre à jour le statut de la candidature
        String statusCode = "retenu".equals(finalStatus) ? "retenu" : "non_retenu";
        applicationRepository.findById(applicationId).ifPresent(app ->
            statusRepository.findByCode(statusCode).ifPresent(s -> {
                app.setStatus(s);
                applicationRepository.save(app);
            })
        );

        activityService.logEvent(applicationId, EventType.DECISION_RECORDED, Map.of(
                "type", "FINAL",
                "finalStatus", finalStatus,
                "decidedBy", actorId.toString()
        ));

        // Notifier tous les membres qui ont donné un avis
        try {
            String deciderName = actor.getName() != null ? actor.getName() : actor.getEmail();
            String statusLabel = switch (finalStatus) {
                case "retenu"     -> "Retenu";
                case "non_retenu" -> "Non retenu";
                case "vivier"     -> "Mis en vivier";
                case "embauche"   -> "Embauché";
                default           -> finalStatus;
            };
            decisionInputRepository
                .findByApplicationIdAndCompanyIdOrderByCreatedAtAsc(applicationId, companyId)
                .stream().map(DecisionInput::getAuthorId).distinct()
                .filter(id -> !id.equals(actorId))
                .forEach(targetId -> notificationService.notify(
                    targetId, companyId,
                    InternalNotification.Type.FINAL_DECISION_RECORDED,
                    "Décision finale enregistrée",
                    deciderName + " — décision : " + statusLabel,
                    "APPLICATION", applicationId
                ));
        } catch (Exception ignored) {}

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

    private String buildCandidateName(Application app) {
        if (app == null || app.getCandidate() == null) return "ce candidat";
        String first = app.getCandidate().getFirstName() != null ? app.getCandidate().getFirstName() : "";
        String last  = app.getCandidate().getLastName()  != null ? app.getCandidate().getLastName()  : "";
        return (first + " " + last).trim();
    }

    private DecisionInputDTO toInputDTO(DecisionInput d, String authorName, UUID actorId) {
        DecisionInputDTO dto = new DecisionInputDTO();
        dto.setId(d.getId());
        dto.setApplicationId(d.getApplicationId());
        dto.setAuthorId(d.getAuthorId());
        dto.setAuthorName(authorName);
        dto.setOwn(actorId != null && actorId.equals(d.getAuthorId()));
        dto.setSentiment(d.getSentiment().name());
        dto.setComment(d.getComment());
        dto.setConfidence(d.getConfidence());
        dto.setCreatedAt(d.getCreatedAt());
        return dto;
    }
}
