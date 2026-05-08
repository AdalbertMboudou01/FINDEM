package com.memoire.assistant.service;

import com.memoire.assistant.dto.ChatAnswerAnalysisDTO;
import com.memoire.assistant.model.AIPersona;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.CommentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class AIFindAssistService {

    private static final Logger log = LoggerFactory.getLogger(AIFindAssistService.class);

    @Autowired private CommentRepository commentRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private AIPersonaCommentService aiPersonaCommentService;

    public void postAnalysisIfNeeded(UUID applicationId, ChatAnswerAnalysisDTO analysis) {
        try {
            var application = applicationRepository.findById(applicationId).orElse(null);
            if (application == null) return;

            UUID companyId = null;
            if (application.getJob() != null && application.getJob().getCompany() != null) {
                companyId = application.getJob().getCompany().getCompanyId();
            }
            if (companyId == null) return;

            String body = buildBody(analysis);

            // Update existing comment if present, otherwise create
            var existing = commentRepository.findByApplication_ApplicationIdAndAuthorId(
                    applicationId, AIPersona.FINDEM_ASSIST_ID);

            if (existing.isPresent()) {
                var comment = existing.get();
                comment.setBody(body.strip());
                comment.setUpdatedAt(java.time.LocalDateTime.now());
                commentRepository.save(comment);
            } else {
                aiPersonaCommentService.postInternalComment(
                        applicationId,
                        companyId,
                        AIPersona.FINDEM_ASSIST_ID,
                        body,
                        Map.of("persona", "FINDEM_ASSIST"));
            }

        } catch (Exception e) {
            log.warn("AIFindAssistService failed for applicationId={}: {}", applicationId, e.getMessage());
        }
    }

    private String buildBody(ChatAnswerAnalysisDTO a) {
        StringBuilder sb = new StringBuilder("**[FindemAssist — Analyse initiale]**\n\n");

        sb.append("**Motivation :** ").append(label(a.getMotivationLevel(), "HIGH", "MEDIUM", "LOW",
                "Élevée", "Moyenne", "Faible")).append("\n");
        sb.append("**Niveau technique :** ").append(label(a.getTechnicalLevel(), "STRONG", "MEDIUM", "WEAK",
                "Solide", "Correct", "Insuffisant")).append("\n");
        sb.append("**Recommandation :** ").append(labelAction(a.getRecommendedAction())).append("\n");

        if (a.getStrengths() != null && !a.getStrengths().isEmpty()) {
            sb.append("\n**Points forts :**\n");
            a.getStrengths().forEach(s -> sb.append("- ").append(s).append("\n"));
        }

        if (a.getPointsToConfirm() != null && !a.getPointsToConfirm().isEmpty()) {
            sb.append("\n**À vérifier :**\n");
            a.getPointsToConfirm().forEach(p -> sb.append("- ").append(p).append("\n"));
        }

        if (a.getInconsistencies() != null && !a.getInconsistencies().isEmpty()) {
            sb.append("\n⚠️ **Incohérences détectées :**\n");
            a.getInconsistencies().forEach(i -> sb.append("- ").append(i).append("\n"));
        }

        if (a.getRecruiterGuidance() != null && !a.getRecruiterGuidance().isBlank()) {
            sb.append("\n*").append(a.getRecruiterGuidance()).append("*");
        }

        return sb.toString().strip();
    }

    private String label(String value, String high, String medium, String low,
                         String highLabel, String mediumLabel, String lowLabel) {
        if (high.equalsIgnoreCase(value)) return highLabel;
        if (medium.equalsIgnoreCase(value)) return mediumLabel;
        if (low.equalsIgnoreCase(value)) return lowLabel;
        return value != null ? value : "Non déterminé";
    }

    private String labelAction(String action) {
        if ("PRIORITY".equalsIgnoreCase(action)) return "Candidature prioritaire";
        if ("REVIEW".equalsIgnoreCase(action)) return "À examiner";
        if ("REJECT".equalsIgnoreCase(action)) return "Ne correspond pas au profil";
        return action != null ? action : "Indéterminé";
    }
}
