package com.memoire.assistant.service;

import com.memoire.assistant.dto.ChatAnswerAnalysisDTO;
import com.memoire.assistant.model.Application;
import com.memoire.assistant.model.ChatAnswer;
import com.memoire.assistant.model.Job;
import com.memoire.assistant.repository.ChatAnswerRepository;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.CandidateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasicServiceTest {

    @Mock
    private ChatAnswerRepository chatAnswerRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private CandidateRepository candidateRepository;

    @Mock
    private SemanticExtractionService semanticExtractionService;

    @Mock
    private AIFindAssistService aiFindAssistService;

    @InjectMocks
    private ChatAnswerService chatAnswerService;

    private UUID applicationId;
    private Application application;
    private Job job;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
        
        // Setup Job
        job = new Job();
        job.setJobId(UUID.randomUUID());
        job.setTitle("Développeur Java");
        job.setLocation("Paris");

        // Setup Application
        application = new Application();
        application.setApplicationId(applicationId);
        application.setJob(job);
    }

    @Test
    void testAnalyzeChatAnswers_BasicFunctionality() {
        // Given
        List<ChatAnswer> answers = Arrays.asList(
            createChatAnswer("Motivation ?", "Je suis passionné par le développement"),
            createChatAnswer("Compétences ?", "Java, Spring, SQL"),
            createChatAnswer("Disponibilité ?", "Immédiatement")
        );

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(chatAnswerRepository.findByApplication_ApplicationId(applicationId)).thenReturn(answers);

        // When
        ChatAnswerAnalysisDTO result = chatAnswerService.analyzeChatAnswers(applicationId);

        // Then
        assertNotNull(result);
        assertEquals(applicationId.toString(), result.getApplicationId());
        assertNotNull(result.getMotivationLevel());
        assertNotNull(result.getTechnicalLevel());
        assertNotNull(result.getAvailabilityStatus());
        assertTrue(result.getCompletenessScore() >= 0.0);

        verify(applicationRepository).findById(applicationId);
        verify(chatAnswerRepository).findByApplication_ApplicationId(applicationId);
    }

    @Test
    void testAnalyzeChatAnswers_ApplicationNotFound() {
        // Given
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            chatAnswerService.analyzeChatAnswers(applicationId);
        });

        verify(applicationRepository).findById(applicationId);
        verify(chatAnswerRepository, never()).findByApplication_ApplicationId(any());
    }

    @Test
    void testValidateAnswerCompleteness() {
        // Given
        List<ChatAnswer> completeAnswers = Arrays.asList(
            createChatAnswer("Motivation ?", "Je suis très motivé"),
            createChatAnswer("Compétences ?", "Java, Spring, React"),
            createChatAnswer("Projets ?", "Application e-commerce"),
            createChatAnswer("Disponibilité ?", "3 jours/semaine"),
            createChatAnswer("Localisation ?", "Paris")
        );

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(chatAnswerRepository.findByApplication_ApplicationId(applicationId)).thenReturn(completeAnswers);

        // When
        boolean result = chatAnswerService.validateAnswerCompleteness(applicationId);

        // Then
        assertTrue(result);

        verify(applicationRepository).findById(applicationId);
        verify(chatAnswerRepository).findByApplication_ApplicationId(applicationId);
    }

    @Test
    void testExtractStructuredAnswers() {
        // Given
        List<ChatAnswer> answers = Arrays.asList(
            createChatAnswer("motivation", "Je suis passionné par l'innovation"),
            createChatAnswer("projets", "J'ai développé une plateforme e-commerce"),
            createChatAnswer("Disponibilité ?", "Je suis disponible immédiatement")
        );

        when(chatAnswerRepository.findByApplication_ApplicationId(applicationId)).thenReturn(answers);

        // When
        Map<String, String> result = chatAnswerService.extractStructuredAnswers(applicationId);

        // Then - Vérifications basiques
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(3, result.size()); // 3 réponses structurées

        // Vérifier que les réponses contiennent les textes attendus
        assertTrue(result.get("motivation").contains("passionné"));
        assertTrue(result.get("projets").contains("e-commerce"));
        assertTrue(result.get("disponibilite").contains("immédiatement"));

        verify(chatAnswerRepository).findByApplication_ApplicationId(applicationId);
        // Pas de vérification sur applicationRepository car non utilisé dans extractStructuredAnswers
    }

    private ChatAnswer createChatAnswer(String question, String answer) {
        ChatAnswer chatAnswer = new ChatAnswer();
        chatAnswer.setQuestionText(question);
        chatAnswer.setAnswerText(answer);
        chatAnswer.setApplication(application);
        return chatAnswer;
    }
}
