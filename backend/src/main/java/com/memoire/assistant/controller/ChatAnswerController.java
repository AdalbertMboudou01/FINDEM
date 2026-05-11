package com.memoire.assistant.controller;

import com.memoire.assistant.dto.ChatAnswerAnalysisDTO;
import com.memoire.assistant.dto.ChatAnswerDTO;
import com.memoire.assistant.dto.AnalysisFactFeedbackRequest;
import com.memoire.assistant.dto.AnalysisFactFeedbackResponse;
import com.memoire.assistant.dto.AnalysisQualityMetricsDTO;
import com.memoire.assistant.model.Application;
import com.memoire.assistant.model.ChatAnswer;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.ChatAnswerRepository;
import com.memoire.assistant.model.ApplicationActivity.EventType;
import com.memoire.assistant.service.ApplicationActivityService;
import com.memoire.assistant.service.ChatAnswerService;
import com.memoire.assistant.service.AnalysisFactFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat-answers")
@Tag(name = "Chat Answers", description = "API pour la gestion et l'analyse des réponses du chatbot")
public class ChatAnswerController {
    
    @Autowired
    private ChatAnswerService chatAnswerService;

    @Autowired
    private AnalysisFactFeedbackService analysisFactFeedbackService;

    @Autowired
    private ApplicationActivityService activityService;
    
    @Autowired
    private ChatAnswerRepository chatAnswerRepository;
    
    @Autowired
    private ApplicationRepository applicationRepository;
    
    @PostMapping("/submit")
    @Operation(summary = "Soumettre une réponse du chatbot", description = "Enregistre une réponse du candidat à une question du chatbot")
    public ResponseEntity<Map<String, String>> submitAnswer(@Valid @RequestBody ChatAnswerDTO chatAnswerDTO) {
        try {
            // Récupérer l'application
            Application application = applicationRepository.findById(chatAnswerDTO.getApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("Application non trouvée: " + chatAnswerDTO.getApplicationId()));
            
            // Créer la réponse
            ChatAnswer chatAnswer = new ChatAnswer();
            chatAnswer.setApplication(application);
            chatAnswer.setQuestionKey(chatAnswerDTO.getQuestionKey());
            chatAnswer.setQuestionText(chatAnswerDTO.getQuestionText());
            chatAnswer.setAnswerText(chatAnswerDTO.getAnswer());
            chatAnswer.setNormalizedValue(chatAnswerDTO.getNormalizedValue());
            chatAnswer.setRequired(chatAnswerDTO.getRequired() != null ? chatAnswerDTO.getRequired() : Boolean.FALSE);
            chatAnswer.setAnsweredAt(chatAnswerDTO.getAnsweredAt() != null ? chatAnswerDTO.getAnsweredAt() : java.time.LocalDateTime.now());
            
            // Sauvegarder
            chatAnswerRepository.save(chatAnswer);
            
            // Logger l'activité
            try {
                activityService.logEvent(chatAnswerDTO.getApplicationId(), EventType.ANSWER_SUBMITTED,
                    Map.of("questionKey", chatAnswerDTO.getQuestionKey(),
                           "answerLength", chatAnswerDTO.getAnswer().length()));
                ;
            } catch (Exception ignored) {}
            
            return ResponseEntity.ok(Map.of(
                "message", "Réponse enregistrée avec succès",
                "answerId", chatAnswer.getAnswerId().toString(),
                "status", "success"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", "Erreur lors de l'enregistrement: " + e.getMessage(),
                "status", "error"
            ));
        }
    }
    
    @PostMapping("/submit-batch")
    @Operation(summary = "Soumettre plusieurs réponses", description = "Enregistre un lot de réponses du candidat")
    public ResponseEntity<Map<String, Object>> submitAnswers(@Valid @RequestBody List<ChatAnswerDTO> chatAnswers) {
        try {
            // Récupérer l'application (toutes les réponses devraient avoir la même applicationId)
            UUID applicationId = chatAnswers.get(0).getApplicationId();
            Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application non trouvée: " + applicationId));
            
            // Créer et sauvegarder toutes les réponses
            List<UUID> savedAnswerIds = new java.util.ArrayList<>();
            for (ChatAnswerDTO dto : chatAnswers) {
                ChatAnswer chatAnswer = new ChatAnswer();
                chatAnswer.setApplication(application);
                chatAnswer.setQuestionKey(dto.getQuestionKey());
                chatAnswer.setQuestionText(dto.getQuestionText());
                chatAnswer.setAnswerText(dto.getAnswer());
                chatAnswer.setNormalizedValue(dto.getNormalizedValue());
                chatAnswer.setRequired(dto.getRequired());
                chatAnswer.setAnsweredAt(dto.getAnsweredAt() != null ? dto.getAnsweredAt() : java.time.LocalDateTime.now());
                
                ChatAnswer saved = chatAnswerRepository.save(chatAnswer);
                savedAnswerIds.add(saved.getAnswerId());
            }
            
            // Invalider le cache d'analyse — les nouvelles réponses le rendent obsolète
            try { chatAnswerService.invalidateSemanticCache(applicationId); } catch (Exception ignored) {}

            // Logger l'activité
            try {
                activityService.logEvent(applicationId, EventType.BATCH_ANSWERS_SUBMITTED,
                    Map.of("answerCount", chatAnswers.size(),
                           "answerIds", savedAnswerIds.stream().map(UUID::toString).collect(java.util.stream.Collectors.toList()))
                );
            } catch (Exception ignored) {}

            return ResponseEntity.ok(Map.of(
                "message", chatAnswers.size() + " réponses enregistrées avec succès",
                "answerCount", chatAnswers.size(),
                "answerIds", savedAnswerIds.stream().map(UUID::toString).collect(java.util.stream.Collectors.toList()),
                "status", "success"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", "Erreur lors de l'enregistrement: " + e.getMessage(),
                "status", "error"
            ));
        }
    }
    
    @GetMapping("/analyze/{applicationId}")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Analyser les réponses d'une candidature", description = "Analyse complète des réponses du chatbot pour une candidature donnée")
    public ResponseEntity<ChatAnswerAnalysisDTO> analyzeAnswers(
            @Parameter(description = "ID de la candidature") @PathVariable UUID applicationId) {
        try {
            ChatAnswerAnalysisDTO analysis = chatAnswerService.analyzeChatAnswers(applicationId);
            try {
                activityService.logEvent(applicationId, EventType.AI_ANALYSIS_DONE,
                        Map.of("completeness", analysis.getCompletenessScore(),
                               "recommended_action", analysis.getRecommendedAction() != null ? analysis.getRecommendedAction() : ""));
            } catch (Exception ignored) {}
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    @GetMapping("/extract/{applicationId}")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Extraire les réponses structurées", description = "Extrait et structure les réponses du chatbot par catégorie")
    public ResponseEntity<Map<String, String>> extractStructuredAnswers(
            @Parameter(description = "ID de la candidature") @PathVariable UUID applicationId) {
        try {
            Map<String, String> structuredAnswers = chatAnswerService.extractStructuredAnswers(applicationId);
            return ResponseEntity.ok(structuredAnswers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    @GetMapping("/validate/{applicationId}")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Valider la complétude des réponses", description = "Vérifie si les réponses sont complètes et exploitables")
    public ResponseEntity<Map<String, Object>> validateCompleteness(
            @Parameter(description = "ID de la candidature") @PathVariable UUID applicationId) {
        try {
            boolean isComplete = chatAnswerService.validateAnswerCompleteness(applicationId);
            ChatAnswerAnalysisDTO analysis = chatAnswerService.analyzeChatAnswers(applicationId);
            
            return ResponseEntity.ok(Map.of(
                "isComplete", isComplete,
                "completenessScore", analysis.getCompletenessScore(),
                "missingInformation", analysis.getMissingInformation(),
                "recommendedAction", analysis.getRecommendedAction()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
    
    @GetMapping("/summary/{applicationId}")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Obtenir un résumé des réponses", description = "Génère un résumé textuel des réponses du candidat")
    public ResponseEntity<Map<String, Object>> getAnswersSummary(
            @Parameter(description = "ID de la candidature") @PathVariable UUID applicationId) {
        try {
            ChatAnswerAnalysisDTO analysis = chatAnswerService.analyzeChatAnswers(applicationId);
            return ResponseEntity.ok(buildSummaryMap(applicationId, analysis));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PostMapping("/summary/batch")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Résumés en lot pour plusieurs candidatures", description = "Retourne les résumés d'analyse pour une liste d'IDs de candidatures en une seule requête")
    public ResponseEntity<Map<String, Object>> getAnswersSummaryBatch(@RequestBody List<String> applicationIdStrings) {
        try {
            List<UUID> applicationIds = applicationIdStrings.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(UUID::fromString)
                .collect(Collectors.toList());

            Map<UUID, ChatAnswerAnalysisDTO> analyses = chatAnswerService.analyzeChatAnswersBatch(applicationIds);

            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<UUID, ChatAnswerAnalysisDTO> entry : analyses.entrySet()) {
                result.put(entry.getKey().toString(), buildSummaryMap(entry.getKey(), entry.getValue()));
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage() == null ? "Erreur batch" : e.getMessage()));
        }
    }

    private Map<String, Object> buildSummaryMap(UUID applicationId, ChatAnswerAnalysisDTO analysis) {
        List<AnalysisFactFeedbackResponse> latestFeedback = analysisFactFeedbackService.getLatestFeedbackByApplication(applicationId);

        Set<String> reviewedKeys = latestFeedback.stream()
            .map(item -> ((item.getDimension() == null ? "general" : item.getDimension()) + "::" + (item.getFinding() == null ? "" : item.getFinding())))
            .collect(Collectors.toSet());

        int totalFacts = analysis.getSemanticFacts() == null ? 0 : analysis.getSemanticFacts().size();
        int reviewedFacts = analysis.getSemanticFacts() == null ? 0 : (int) analysis.getSemanticFacts().stream()
            .filter(f -> reviewedKeys.contains((f.getDimension() == null ? "general" : f.getDimension()) + "::" + (f.getFinding() == null ? "" : f.getFinding())))
            .count();
        double completionRate = totalFacts == 0 ? 0.0 : ((double) reviewedFacts / (double) totalFacts);

        Map<String, Object> analysisSchema = Map.of(
            "version", analysis.getAnalysisSchemaVersion() == null ? "phase1.v1" : analysis.getAnalysisSchemaVersion(),
            "facts", analysis.getSemanticFacts() == null ? List.of() : analysis.getSemanticFacts(),
            "missingInformation", analysis.getMissingInformation() == null ? List.of() : analysis.getMissingInformation(),
            "contradictions", analysis.getInconsistencies() == null ? List.of() : analysis.getInconsistencies(),
            "fallbackUsed", analysis.isSemanticFallbackUsed()
        );

        Map<String, Object> analysisReviewCoverage = Map.of(
            "reviewedFacts", reviewedFacts,
            "totalFacts", totalFacts,
            "completionRate", completionRate
        );

        return Map.ofEntries(
            Map.entry("applicationId", applicationId.toString()),
            Map.entry("motivationLevel", analysis.getMotivationLevel()),
            Map.entry("technicalLevel", analysis.getTechnicalLevel()),
            Map.entry("hasProjectDetails", analysis.isHasProjectDetails()),
            Map.entry("hasClearAvailability", analysis.isHasClearAvailability()),
            Map.entry("locationMatch", analysis.getLocationMatch()),
            Map.entry("completenessScore", analysis.getCompletenessScore()),
            Map.entry("recommendedAction", analysis.getRecommendedAction()),
            Map.entry("motivationSummary", analysis.getMotivationSummary()),
            Map.entry("motivationAssessment", analysis.getMotivationAssessment() == null ? "" : analysis.getMotivationAssessment()),
            Map.entry("projectAssessment", analysis.getProjectAssessment() == null ? "" : analysis.getProjectAssessment()),
            Map.entry("githubSummary", analysis.getGithubSummary() == null ? "" : analysis.getGithubSummary()),
            Map.entry("githubAssessment", analysis.getGithubAssessment() == null ? "" : analysis.getGithubAssessment()),
            Map.entry("availabilityAssessment", analysis.getAvailabilityAssessment() == null ? "" : analysis.getAvailabilityAssessment()),
            Map.entry("locationAssessment", analysis.getLocationAssessment() == null ? "" : analysis.getLocationAssessment()),
            Map.entry("mentionedProjects", analysis.getMentionedProjects()),
            Map.entry("technicalSkills", analysis.getTechnicalSkills()),
            Map.entry("strengths", analysis.getStrengths() == null ? List.of() : analysis.getStrengths()),
            Map.entry("missingInformation", analysis.getMissingInformation()),
            Map.entry("inconsistencies", analysis.getInconsistencies()),
            Map.entry("pointsToConfirm", analysis.getPointsToConfirm() == null ? List.of() : analysis.getPointsToConfirm()),
            Map.entry("followUpQuestions", analysis.getFollowUpQuestions() == null ? List.of() : analysis.getFollowUpQuestions()),
            Map.entry("recruiterGuidance", analysis.getRecruiterGuidance() == null ? "" : analysis.getRecruiterGuidance()),
            Map.entry("analysisSchema", analysisSchema),
            Map.entry("analysisReviewCoverage", analysisReviewCoverage)
        );
    }

    @GetMapping("/feedback/{applicationId}")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Lister les corrections recruteur", description = "Retourne l'historique des corrections de constats pour une candidature")
    public ResponseEntity<?> getAnalysisFeedback(
            @Parameter(description = "ID de la candidature") @PathVariable UUID applicationId) {
        try {
            return ResponseEntity.ok(analysisFactFeedbackService.getFeedbackByApplication(applicationId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", e.getMessage() == null ? "Erreur lors de la lecture des corrections" : e.getMessage(),
                "status", "error"
            ));
        }
    }

    @GetMapping("/feedback/{applicationId}/latest")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Lister les dernieres corrections par constat", description = "Retourne uniquement la derniere decision pour chaque constat")
    public ResponseEntity<?> getLatestAnalysisFeedback(
            @Parameter(description = "ID de la candidature") @PathVariable UUID applicationId) {
        try {
            return ResponseEntity.ok(analysisFactFeedbackService.getLatestFeedbackByApplication(applicationId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", e.getMessage() == null ? "Erreur lors de la lecture des dernieres corrections" : e.getMessage(),
                "status", "error"
            ));
        }
    }

    @GetMapping("/quality-metrics")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Métriques qualité de l'analyse chatbot", description = "Retourne les indicateurs de qualité de l'analyse selon les validations/corrections recruteur")
    public ResponseEntity<?> getQualityMetrics(
            @Parameter(description = "Fenêtre glissante en jours (1-365)") @RequestParam(name = "days", defaultValue = "30") int days) {
        try {
            AnalysisQualityMetricsDTO metrics = analysisFactFeedbackService.getQualityMetrics(days);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", e.getMessage() == null ? "Erreur lors du calcul des métriques" : e.getMessage(),
                "status", "error"
            ));
        }
    }

    @PostMapping("/feedback/{applicationId}")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Ajouter une correction recruteur", description = "Enregistre une validation/correction/rejet d'un constat d'analyse")
    public ResponseEntity<?> saveAnalysisFeedback(
            @Parameter(description = "ID de la candidature") @PathVariable UUID applicationId,
            @RequestBody AnalysisFactFeedbackRequest request) {
        try {
            return ResponseEntity.ok(analysisFactFeedbackService.saveFeedback(applicationId, request));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", e.getMessage() == null ? "Erreur lors de l'enregistrement de la correction" : e.getMessage(),
                "status", "error"
            ));
        }
    }
}
