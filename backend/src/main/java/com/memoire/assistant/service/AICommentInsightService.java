package com.memoire.assistant.service;

import com.memoire.assistant.dto.ChatAnswerAnalysisDTO;
import com.memoire.assistant.model.AIPersona;
import com.memoire.assistant.model.Comment;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.CommentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AICommentInsightService {

    private static final Logger log = LoggerFactory.getLogger(AICommentInsightService.class);
    private static final UUID AI_AUTHOR_ID = AIPersona.FINDEM_LOOKER_ID;

    @Autowired private CommentRepository commentRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private ChatAnswerService chatAnswerService;
    @Autowired private AIClientService aiClientService;
    @Autowired private AIPersonaCommentService aiPersonaCommentService;

    public void analyzeAndInsert(UUID applicationId, UUID companyId, String newCommentBody) {
        if (!aiClientService.isEnabled()) {
            return;
        }
        try {
            List<Comment> allComments = commentRepository
                .findByApplication_ApplicationIdAndCompanyIdOrderByCreatedAtAsc(applicationId, companyId);

            // Anti-spam : si l'un des 3 derniers commentaires est IA, on ne reposte pas
            int size = allComments.size();
            List<Comment> last3 = allComments.subList(Math.max(0, size - 3), size);
            boolean recentAiComment = last3.stream()
                .anyMatch(c -> Comment.AuthorType.AI_SYSTEM.equals(c.getAuthorType()));
            if (recentAiComment) {
                return;
            }

            // Contexte : 5 derniers commentaires humains
            List<Comment> humanComments = allComments.stream()
                .filter(c -> !Comment.AuthorType.AI_SYSTEM.equals(c.getAuthorType()))
                .collect(Collectors.toList());
            List<Comment> last5Human = humanComments.subList(Math.max(0, humanComments.size() - 5), humanComments.size());

            ChatAnswerAnalysisDTO analysis = chatAnswerService.analyzeChatAnswers(applicationId);

            // Passe 1 : détection de contradictions
            String prompt = buildPrompt(analysis, last5Human, newCommentBody);
            String response = callOpenAI(prompt);

            var application = applicationRepository.findById(applicationId).orElse(null);
            if (application == null) return;

            if (response != null && !response.strip().equalsIgnoreCase("SKIP")) {
                postAiComment(application, companyId, AI_AUTHOR_ID, response.strip(), applicationId);
            }

            // Passe 2 : détection de biais (indépendante)
            detectAndPostBias(applicationId, companyId, application, last5Human, newCommentBody);

        } catch (Exception e) {
            log.warn("AI comment insight failed for applicationId={}: {}", applicationId, e.getMessage());
        }
    }

    private void detectAndPostBias(UUID applicationId, UUID companyId,
                                   com.memoire.assistant.model.Application application,
                                   List<Comment> humanComments, String newCommentBody) {
        try {
            // Anti-spam biais : pas d'alerte récente dans les 5 derniers commentaires
            List<Comment> all = commentRepository
                .findByApplication_ApplicationIdAndCompanyIdOrderByCreatedAtAsc(applicationId, companyId);
            int total = all.size();
            List<Comment> last5 = all.subList(Math.max(0, total - 5), total);
            boolean recentBiasAlert = last5.stream()
                .anyMatch(c -> c.getBody() != null && c.getBody().contains("[FindemLooker — Alerte biais"));
            if (recentBiasAlert) return;

            String biasPrompt = buildBiasPrompt(humanComments, newCommentBody);
            String biasResponse = aiClientService.complete(
                "Tu es un détecteur de biais dans les processus de recrutement. " +
                "Identifie si les commentaires contiennent un biais cognitif potentiel " +
                "(biais d'affinité, effet de halo, stéréotype, jugement subjectif non fondé sur des faits). " +
                "Si tu détectes un signal clair, cite le passage concerné et nomme le type de biais en 1-2 phrases. " +
                "Sinon réponds exactement SKIP.",
                biasPrompt,
                0
            );

            if (biasResponse != null && !biasResponse.strip().equalsIgnoreCase("SKIP")) {
                String body = "**[FindemLooker — Alerte biais potentiel]**\n\n" + biasResponse.strip();
                postAiComment(application, companyId, AI_AUTHOR_ID, body, applicationId);
            }
        } catch (Exception e) {
            log.warn("Bias detection failed for applicationId={}: {}", applicationId, e.getMessage());
        }
    }

    private void postAiComment(com.memoire.assistant.model.Application application,
                                UUID companyId, UUID authorId, String body, UUID applicationId) {
        aiPersonaCommentService.postInternalComment(applicationId, companyId, authorId, body,
            Map.of("persona", "FINDEM_LOOKER"));
    }

    private String buildPrompt(ChatAnswerAnalysisDTO analysis, List<Comment> previousComments, String newComment) {
        StringBuilder sb = new StringBuilder();

        if (analysis != null) {
            sb.append("Analyse initiale du candidat : motivation=").append(analysis.getMotivationLevel())
              .append(", technique=").append(analysis.getTechnicalLevel())
              .append(", recommandation IA=").append(analysis.getRecommendedAction()).append("\n\n");
        }

        if (!previousComments.isEmpty()) {
            sb.append("Commentaires précédents de l'équipe :\n");
            previousComments.forEach(c -> sb.append("- ").append(c.getBody()).append("\n"));
            sb.append("\n");
        }

        sb.append("Nouveau commentaire : ").append(newComment).append("\n\n");
        sb.append("Si tu détectes une contradiction avec l'analyse ou un angle critique non abordé, ")
          .append("indique-le en 1-2 phrases. Sinon réponds exactement SKIP.");

        return sb.toString();
    }

    private String buildBiasPrompt(List<Comment> previousComments, String newComment) {
        StringBuilder sb = new StringBuilder();
        if (!previousComments.isEmpty()) {
            sb.append("Commentaires précédents de l'équipe :\n");
            previousComments.forEach(c -> sb.append("- ").append(c.getBody()).append("\n"));
            sb.append("\n");
        }
        sb.append("Nouveau commentaire : ").append(newComment);
        return sb.toString();
    }

    private String callOpenAI(String userPrompt) {
        return aiClientService.complete(
            "Tu es un assistant de recrutement silencieux. Tu interviens UNIQUEMENT si tu détectes " +
            "une contradiction importante ou un angle critique non abordé par l'équipe. " +
            "Si tout est cohérent ou si tu n'as rien d'utile à ajouter, réponds exactement SKIP.",
            userPrompt,
            0
        );
    }
}
