package com.memoire.assistant.service;

import com.memoire.assistant.model.AIPersona;
import com.memoire.assistant.model.ApplicationActivity.EventType;
import com.memoire.assistant.model.Task;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class AITaskExecutionService {

    private static final Logger log = LoggerFactory.getLogger(AITaskExecutionService.class);

    @Autowired private TaskRepository taskRepository;
    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private ChatAnswerService chatAnswerService;
    @Autowired private ApplicationActivityService activityService;
    @Autowired private AIClientService aiClientService;
    @Autowired private AIPersonaCommentService aiPersonaCommentService;

    @Async
    public void execute(UUID taskId, UUID applicationId, UUID companyId, String taskType) {
        if (!aiClientService.isEnabled()) {
            return;
        }
        try {
            Task task = taskRepository.findById(taskId).orElse(null);
            if (task == null) return;

            var application = applicationRepository.findById(applicationId).orElse(null);
            if (application == null) return;

            var analysis = chatAnswerService.analyzeChatAnswers(applicationId);
            String candidateName = application.getCandidate() != null
                    ? application.getCandidate().getFirstName() + " " + application.getCandidate().getLastName()
                    : "le candidat";
            String jobTitle = application.getJob() != null ? application.getJob().getTitle() : "le poste";

            String prompt = buildPrompt(taskType, candidateName, jobTitle, analysis, task.getDescription());
            if (prompt == null) {
                log.warn("AITaskExecutionService: type de tâche inconnu '{}' — ignoré", taskType);
                return;
            }

            String result = aiClientService.complete(
                    "Tu es FindemWorker, un assistant RH expert. "
                    + "Tu rédiges des contenus professionnels en français pour aider les recruteurs. "
                    + "Tes réponses sont directement utilisables sans modification.",
                    prompt,
                    0.4
            );
            if (result == null || result.isBlank()) return;

            // Persist result on the task
            task.setAiResult(result.strip());
            task.setStatus(Task.Status.DONE);
            taskRepository.save(task);

            String body = "**[FindemWorker — " + labelFor(taskType) + "]**\n\n" + result.strip();
            aiPersonaCommentService.postInternalComment(applicationId, companyId, AIPersona.FINDEM_WORKER_ID, body,
                    Map.of("persona", "FINDEM_WORKER", "taskId", taskId.toString(), "taskType", taskType));

            activityService.logSystemEvent(applicationId, companyId, EventType.TASK_DONE, Map.of(
                    "taskId", taskId.toString(),
                    "taskType", taskType,
                    "author_type", "AI_SYSTEM"
            ));

        } catch (Exception e) {
            log.warn("AITaskExecutionService failed for taskId={}: {}", taskId, e.getMessage());
        }
    }

    private String buildPrompt(String taskType, String candidateName, String jobTitle,
                                com.memoire.assistant.dto.ChatAnswerAnalysisDTO analysis, String extraContext) {
        String analysisContext = analysis != null
                ? "Analyse IA : motivation=" + analysis.getMotivationLevel()
                  + ", technique=" + analysis.getTechnicalLevel()
                  + ", recommandation=" + analysis.getRecommendedAction() + ".\n"
                : "";

        String ctx = analysisContext
                + (extraContext != null && !extraContext.isBlank() ? "Contexte supplémentaire : " + extraContext + "\n" : "");

        return switch (taskType) {
            case "EMAIL_CONFIRMATION" -> ctx +
                    "Rédige un e-mail professionnel en français pour confirmer la réception de la candidature de "
                    + candidateName + " pour le poste de " + jobTitle + ". "
                    + "Ton chaleureux, concis, 3-4 phrases maximum.";

            case "EMAIL_REJECTION" -> ctx +
                    "Rédige un e-mail de refus bienveillant en français pour " + candidateName
                    + " qui a postulé pour le poste de " + jobTitle + ". "
                    + "Sois respectueux, sincère, encourage le candidat, 4-5 phrases maximum.";

            case "EMAIL_INVITATION" -> ctx +
                    "Rédige un e-mail d'invitation à un entretien en français pour " + candidateName
                    + " concernant le poste de " + jobTitle + ". "
                    + "Inclure un rappel que les détails (date, heure, lieu) seront précisés séparément. "
                    + "Ton professionnel et accueillant, 4-5 phrases.";

            case "INTERVIEW_QUESTIONS" -> ctx +
                    "Génère 5 questions d'entretien pertinentes en français pour évaluer " + candidateName
                    + " pour le poste de " + jobTitle + ". "
                    + "Mix : 2 questions techniques, 2 comportementales, 1 motivation. "
                    + "Format : liste numérotée.";

            case "PROFILE_SUMMARY" -> ctx +
                    "Rédige un résumé synthétique du profil de " + candidateName
                    + " candidat pour le poste de " + jobTitle + " en 3-5 phrases. "
                    + "Basé sur l'analyse IA disponible, points forts et axes d'amélioration.";

            default -> null;
        };
    }

    private String labelFor(String taskType) {
        return switch (taskType) {
            case "EMAIL_CONFIRMATION" -> "Confirmation de candidature";
            case "EMAIL_REJECTION" -> "E-mail de refus";
            case "EMAIL_INVITATION" -> "Invitation à l'entretien";
            case "INTERVIEW_QUESTIONS" -> "Questions d'entretien";
            case "PROFILE_SUMMARY" -> "Résumé du profil";
            default -> taskType;
        };
    }

}
