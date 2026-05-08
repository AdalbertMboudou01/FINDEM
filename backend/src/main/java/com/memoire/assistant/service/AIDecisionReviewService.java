package com.memoire.assistant.service;

import com.memoire.assistant.dto.ChatAnswerAnalysisDTO;
import com.memoire.assistant.model.AIPersona;
import com.memoire.assistant.model.DecisionInput;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.DecisionInputRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AIDecisionReviewService {

    private static final Logger log = LoggerFactory.getLogger(AIDecisionReviewService.class);
    private static final UUID AI_AUTHOR_ID = AIPersona.FINDEM_ASSIST_ID;

    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private DecisionInputRepository decisionInputRepository;
    @Autowired private ChatAnswerService chatAnswerService;
    @Autowired private AIClientService aiClientService;
    @Autowired private AIPersonaCommentService aiPersonaCommentService;

    public String generateReview(UUID applicationId, UUID companyId, String finalStatus, String rationale) {
        if (!aiClientService.isEnabled()) {
            return null;
        }
        try {
            String jobTitle = applicationRepository.findById(applicationId)
                .map(a -> a.getJob() != null ? a.getJob().getTitle() : "poste non précisé")
                .orElse("poste non précisé");

            ChatAnswerAnalysisDTO analysis = chatAnswerService.analyzeChatAnswers(applicationId);

            List<DecisionInput> inputs = decisionInputRepository
                .findByApplicationIdAndCompanyIdOrderByCreatedAtAsc(applicationId, companyId);

            String prompt = buildPrompt(jobTitle, analysis, inputs, finalStatus, rationale);
            String review = aiClientService.complete(prompt, 0);

            if (review != null && !review.isBlank()) {
                postDecisionReviewComment(applicationId, companyId, review.strip());
            }

            return review;

        } catch (Exception e) {
            log.warn("AI decision review failed for applicationId={}: {}", applicationId, e.getMessage());
            return null;
        }
    }

    private String buildPrompt(String jobTitle, ChatAnswerAnalysisDTO analysis,
                                List<DecisionInput> inputs, String finalStatus, String rationale) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tu es un assistant de recrutement. Évalue la cohérence de la décision finale.\n\n");
        sb.append("Poste : ").append(jobTitle).append("\n");

        if (analysis != null) {
            sb.append("Analyse initiale : motivation=").append(analysis.getMotivationLevel())
              .append(", technique=").append(analysis.getTechnicalLevel())
              .append(", recommandation IA=").append(analysis.getRecommendedAction()).append("\n");
        }

        if (inputs != null && !inputs.isEmpty()) {
            sb.append("Avis de l'équipe :\n");
            for (DecisionInput input : inputs) {
                sb.append("- ").append(input.getSentiment().name());
                if (input.getComment() != null && !input.getComment().isBlank()) {
                    sb.append(" : ").append(input.getComment());
                }
                sb.append("\n");
            }
        } else {
            sb.append("Avis de l'équipe : aucun avis enregistré.\n");
        }

        sb.append("Décision proposée : ").append(finalStatus);
        if (rationale != null && !rationale.isBlank()) {
            sb.append(" — ").append(rationale);
        }
        sb.append("\n\nEn 2-3 phrases, indique si la décision est cohérente avec l'analyse et les avis. ")
          .append("Signale toute contradiction ou dimension non abordée. Sois factuel et concis.");

        return sb.toString();
    }

    private void postDecisionReviewComment(UUID applicationId, UUID companyId, String review) {
        String body = "**[FindemAssist — Relecture de décision]**\n\n" + review;
        aiPersonaCommentService.postInternalComment(applicationId, companyId, AI_AUTHOR_ID, body,
            Map.of("persona", "FINDEM_ASSIST", "ai_action", "DECISION_REVIEW"));
    }

}
