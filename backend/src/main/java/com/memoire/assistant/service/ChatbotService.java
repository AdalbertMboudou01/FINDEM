package com.memoire.assistant.service;

import com.memoire.assistant.model.*;
import com.memoire.assistant.dto.*;
import com.memoire.assistant.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.stream.Collectors;

@Service
@Transactional
public class ChatbotService {
    
    @Autowired
    private ChatSessionRepository chatSessionRepository;
    
    // @Autowired
    // private ChatQuestionRepository chatQuestionRepository; // Temporairement désactivé
    
    @Autowired
    private ChatAnswerRepository chatAnswerRepository;
    
    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    private CandidateRepository candidateRepository;
    
    @Autowired
    private ApplicationRepository applicationRepository;
    
    @Autowired
    private ApplicationService applicationService;
    
    // Questions standards du chatbot
    private static final List<ChatQuestionDTO> STANDARD_QUESTIONS = Arrays.asList(
        new ChatQuestionDTO("presentation", "Parlez-nous de vous (formation, parcours)", "text", 1, true, "presentation"),
        new ChatQuestionDTO("motivation", "Pourquoi cette alternance et cette entreprise ?", "text", 2, true, "motivation"),
        new ChatQuestionDTO("technologies", "Quelles technologies maîtrisez-vous ?", "text", 3, true, "technique"),
        new ChatQuestionDTO("projets", "Décrivez vos projets les plus significatifs", "textarea", 4, true, "projet"),
        new ChatQuestionDTO("github", "Avez-vous un GitHub ou portfolio à partager ?", "url", 5, false, "portfolio"),
        new ChatQuestionDTO("disponibilite", "Quelle est votre disponibilité ?", "text", 6, true, "disponibilite"),
        new ChatQuestionDTO("rythme", "Quel rythme d'alternance recherchez-vous ?", "select", 7, true, "alternance"),
        new ChatQuestionDTO("mobilite", "Quelle est votre mobilité géographique ?", "text", 8, true, "localisation")
    );
    
    /**
     * Initialise une session chatbot pour une offre
     */
    public ChatSessionDTO initializeChatSession(String jobSlug, String candidateEmail, String candidateName) {
        // Trouver l'offre par slug
        Optional<Job> job = jobRepository.findBySlug(jobSlug);
        if (!job.isPresent()) {
            throw new IllegalArgumentException("Offre non trouvée");
        }
        
        // Créer ou retrouver le candidat
        Candidate candidate = findOrCreateCandidate(candidateEmail, candidateName);
        
        // Vérifier si une candidature existe déjà
        Optional<Application> existingApplication = applicationRepository
            .findByCandidate_CandidateIdAndJob_JobId(candidate.getCandidateId(), job.get().getJobId());
        
        if (existingApplication.isPresent()) {
            // Retourner la session existante
            ChatSession existingSession = chatSessionRepository
                .findByApplication_ApplicationId(existingApplication.get().getApplicationId())
                .orElse(null);
            if (existingSession != null) {
                return convertToSessionDTO(existingSession);
            }
        }
        
        // Créer la candidature
        Application application = new Application();
        application.setCandidate(candidate);
        application.setJob(job.get());
        application.setCreatedAt(new Date());
        application = applicationRepository.save(application);
        
        // Créer la session chatbot
        ChatSession session = new ChatSession();
        session.setApplication(application);
        session.setStartedAt(new Date());
        session.setScenario("STANDARD_ALTERNANCE");
        session.setLanguage("fr");
        session.setProgress(0);
        session.setCompletionScore(0);
        session.setAbandoned(false);
        
        session = chatSessionRepository.save(session);
        
        // Initialiser les questions pour cette session
        initializeQuestionsForSession(session, job.get());
        
        return convertToSessionDTO(session);
    }
    
    /**
     * Récupère la question courante pour une session
     */
    public ChatQuestionDTO getCurrentQuestion(UUID sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session non trouvée"));
        
        if (session.getProgress() >= STANDARD_QUESTIONS.size()) {
            return null; // Session terminée
        }
        
        return STANDARD_QUESTIONS.get(session.getProgress());
    }
    
    /**
     * Soumet une réponse à une question
     */
    public ChatSessionDTO submitAnswer(UUID sessionId, String questionKey, String answer) {
        ChatSession session = chatSessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session non trouvée"));
        
        // Vérifier que la question est attendue
        ChatQuestionDTO currentQuestion = getCurrentQuestion(sessionId);
        if (currentQuestion == null) {
            throw new IllegalStateException("La session est déjà terminée");
        }
        
        if (!currentQuestion.getKey().equals(questionKey)) {
            throw new IllegalArgumentException("Question non attendue dans cette étape");
        }
        
        // Sauvegarder la réponse
        ChatAnswer chatAnswer = new ChatAnswer();
        chatAnswer.setChatSession(session);
        // chatAnswer.setQuestionId(UUID.randomUUID()); // Temporairement désactivé
        chatAnswer.setAnswerText(answer);
        chatAnswer.setNormalizedValue(normalizeAnswer(questionKey, answer));
        chatAnswer.setAnsweredAt(LocalDateTime.now());
        chatAnswer.setRequired(currentQuestion.isRequired());
        
        chatAnswerRepository.save(chatAnswer);
        
        // Mettre à jour la progression
        session.setProgress(session.getProgress() + 1);
        session.setCompletionScore(calculateCompletionScore(session));
        
        // Si toutes les questions sont répondues, finaliser la session
        if (session.getProgress() >= STANDARD_QUESTIONS.size()) {
            session.setEndedAt(new Date());
            finalizeSession(session);
        }
        
        session = chatSessionRepository.save(session);
        
        return convertToSessionDTO(session);
    }
    
    /**
     * Récupère toutes les réponses d'une session
     */
    public List<ChatAnswerDTO> getSessionAnswers(UUID sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session non trouvée"));
        
        return chatAnswerRepository.findByChatSession_ChatSessionId(sessionId)
            .stream()
            .map(this::convertToAnswerDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * Récupère le résumé d'une session
     */
    public ChatSessionSummaryDTO getSessionSummary(UUID sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session non trouvée"));
        
        List<ChatAnswer> answers = chatAnswerRepository.findByChatSession_ChatSessionId(sessionId);
        
        ChatSessionSummaryDTO summary = new ChatSessionSummaryDTO();
        summary.setSessionId(sessionId);
        summary.setApplicationId(session.getApplication().getApplicationId());
        summary.setCandidateName(session.getApplication().getCandidate().getFirstName() + " " + 
                              session.getApplication().getCandidate().getLastName());
        summary.setJobTitle(session.getApplication().getJob().getTitle());
        summary.setCompanyName(session.getApplication().getJob().getCompany().getName());
        summary.setStartedAt(session.getStartedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        summary.setEndedAt(session.getEndedAt() != null ? session.getEndedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);
        summary.setProgress(session.getProgress());
        summary.setTotalQuestions(STANDARD_QUESTIONS.size());
        summary.setCompletionScore(session.getCompletionScore());
        summary.setAbandoned(session.getAbandoned());
        
        // Ajouter les réponses
        Map<String, String> answersMap = answers.stream()
            .collect(Collectors.toMap(
                answer -> getQuestionKeyFromAnswer(answer),
                ChatAnswer::getAnswerText
            ));
        summary.setAnswers(answersMap);
        
        return summary;
    }
    
    /**
     * Abandonne une session
     */
    public void abandonSession(UUID sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session non trouvée"));
        
        session.setEndedAt(new Date());
        session.setAbandoned(true);
        
        chatSessionRepository.save(session);
    }
    
    /**
     * Récupère les statistiques des sessions pour une offre
     */
    public ChatStatsDTO getJobChatStats(UUID jobId) {
        List<ChatSession> sessions = chatSessionRepository.findAll().stream()
            .filter(session -> session.getApplication().getJob().getJobId().equals(jobId))
            .collect(Collectors.toList());
        
        ChatStatsDTO stats = new ChatStatsDTO();
        stats.setJobId(jobId);
        stats.setTotalSessions(sessions.size());
        stats.setCompletedSessions(sessions.stream()
            .mapToInt(session -> session.getEndedAt() != null && !session.getAbandoned() ? 1 : 0)
            .sum());
        stats.setAbandonedSessions(sessions.stream()
            .mapToInt(session -> session.getAbandoned() ? 1 : 0)
            .sum());
        stats.setAverageCompletionScore(sessions.stream()
            .mapToDouble(session -> session.getCompletionScore())
            .average()
            .orElse(0.0));
        stats.setAverageDuration(sessions.stream()
            .filter(session -> session.getEndedAt() != null)
            .mapToInt(session -> (int) ((session.getEndedAt().getTime() - session.getStartedAt().getTime()) / (1000 * 60)))
            .average()
            .orElse(0.0));
        
        return stats;
    }
    
    // Méthodes privées
    
    private Candidate findOrCreateCandidate(String email, String name) {
        Optional<Candidate> existingCandidate = candidateRepository.findByEmail(email);
        
        if (existingCandidate.isPresent()) {
            return existingCandidate.get();
        }
        
        Candidate newCandidate = new Candidate();
        newCandidate.setEmail(email);
        if (name != null && name.contains(" ")) {
            String[] parts = name.split(" ", 2);
            newCandidate.setFirstName(parts[0]);
            newCandidate.setLastName(parts[1]);
        } else {
            newCandidate.setFirstName(name != null ? name : "Candidat");
            newCandidate.setLastName("");
        }
        newCandidate.setConsent(true);
        newCandidate.setCreatedAt(new Date());
        
        return candidateRepository.save(newCandidate);
    }
    
    private void initializeQuestionsForSession(ChatSession session, Job job) {
        // Pour l'instant, on utilise les questions standards
        // Cette méthode peut être étendue pour personnaliser les questions selon l'offre
    }
    
    private String normalizeAnswer(String questionKey, String answer) {
        if (answer == null) return null;
        
        switch (questionKey) {
            case "technologies":
                return answer.toLowerCase()
                    .replaceAll("[^a-z0-9\\s,]", "")
                    .replaceAll("\\s+", " ")
                    .trim();
            case "github":
                if (answer.trim().isEmpty()) return null;
                if (!answer.startsWith("http")) {
                    return "https://" + answer;
                }
                return answer;
            case "rythme":
                return answer.toUpperCase();
            default:
                return answer.trim();
        }
    }
    
    private int calculateCompletionScore(ChatSession session) {
        int totalQuestions = STANDARD_QUESTIONS.size();
        int answeredQuestions = session.getProgress();
        int requiredQuestions = (int) STANDARD_QUESTIONS.stream()
            .filter(ChatQuestionDTO::isRequired)
            .count();
        
        int answeredRequired = Math.min(answeredQuestions, requiredQuestions);
        
        return (int) ((double) answeredRequired / requiredQuestions * 100);
    }
    
    private void finalizeSession(ChatSession session) {
        // Générer la synthèse de la candidature
        // Déclencher l'analyse des réponses
        // Notifier le recruteur
        
        // Pour l'instant, on marque juste la session comme terminée
        session.setCompletionScore(100);
    }
    
    private String getQuestionKeyFromAnswer(ChatAnswer answer) {
        // Logique pour retrouver la clé de la question à partir de la réponse
        // Pour simplifier, on utilise l'ordre des questions
        List<ChatAnswer> answers = chatAnswerRepository
            .findByChatSession_ChatSessionId(answer.getChatSession().getChatSessionId());
        
        int index = answers.indexOf(answer);
        if (index >= 0 && index < STANDARD_QUESTIONS.size()) {
            return STANDARD_QUESTIONS.get(index).getKey();
        }
        
        return "unknown";
    }
    
    private ChatSessionDTO convertToSessionDTO(ChatSession session) {
        ChatSessionDTO dto = new ChatSessionDTO();
        dto.setSessionId(session.getChatSessionId());
        dto.setApplicationId(session.getApplication().getApplicationId());
        dto.setJobTitle(session.getApplication().getJob().getTitle());
        dto.setCompanyName(session.getApplication().getJob().getCompany().getName());
        dto.setCandidateName(session.getApplication().getCandidate().getFirstName() + " " + 
                           session.getApplication().getCandidate().getLastName());
        dto.setStartedAt(session.getStartedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        dto.setEndedAt(session.getEndedAt() != null ? session.getEndedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);
        dto.setProgress(session.getProgress());
        dto.setTotalQuestions(STANDARD_QUESTIONS.size());
        dto.setCompletionScore(session.getCompletionScore());
        dto.setAbandoned(session.getAbandoned());
        dto.setCurrentQuestion(getCurrentQuestion(session.getChatSessionId()));
        
        return dto;
    }
    
    private ChatAnswerDTO convertToAnswerDTO(ChatAnswer answer) {
        ChatAnswerDTO dto = new ChatAnswerDTO();
        dto.setAnswerId(answer.getAnswerId());
        dto.setSessionId(answer.getChatSession().getChatSessionId());
        dto.setQuestionKey(getQuestionKeyFromAnswer(answer));
        dto.setAnswer(answer.getAnswerText());
        dto.setNormalizedValue(answer.getNormalizedValue());
        dto.setAnsweredAt(answer.getAnsweredAt());
        dto.setRequired(answer.getRequired());
        
        return dto;
    }
}
