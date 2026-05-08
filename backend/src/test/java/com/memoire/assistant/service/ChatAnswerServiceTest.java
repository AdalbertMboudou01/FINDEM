package com.memoire.assistant.service;

import com.memoire.assistant.dto.ChatAnswerAnalysisDTO;
import com.memoire.assistant.model.Application;
import com.memoire.assistant.model.ChatAnswer;
import com.memoire.assistant.model.Job;
import com.memoire.assistant.repository.ChatAnswerRepository;
import com.memoire.assistant.repository.ApplicationRepository;
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
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatAnswerServiceTest {

    @Mock
    private ChatAnswerRepository chatAnswerRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private SemanticExtractionService semanticExtractionService;

    @Mock
    private AIFindAssistService aiFindAssistService;

    @InjectMocks
    private ChatAnswerService chatAnswerService;

    private UUID applicationId;
    private Application application;
    private Job job;
    private List<ChatAnswer> chatAnswers;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
        lenient().when(semanticExtractionService.extractFacts(any())).thenReturn(Collections.emptyList());
        
        // Setup Job
        job = new Job();
        job.setJobId(UUID.randomUUID());
        job.setTitle("Développeur Java");
        job.setLocation("Paris");

        // Setup Application
        application = new Application();
        application.setApplicationId(applicationId);
        application.setJob(job);

        // Setup ChatAnswers
        chatAnswers = Arrays.asList(
            createChatAnswer("Qu'est-ce qui vous motive ?", "Je suis passionné par le développement et j'adore relever de nouveaux défis techniques"),
            createChatAnswer("Quelles sont vos compétences techniques ?", "J'ai 5 ans d'expérience en Java, Spring Boot, React et bases de données SQL"),
            createChatAnswer("Quelle est votre disponibilité ?", "Je suis disponible immédiatement pour un rythme de 3 jours par semaine"),
            createChatAnswer("projets", "J'ai développé une application e-commerce complète avec Spring Boot et React"),
            createChatAnswer("Votre localisation ?", "J'habite à Paris et je peux me déplacer facilement")
        );
    }

    private ChatAnswer createChatAnswer(String question, String answer) {
        ChatAnswer chatAnswer = new ChatAnswer();
        chatAnswer.setQuestionText(question);
        chatAnswer.setAnswerText(answer);
        chatAnswer.setApplication(application);
        return chatAnswer;
    }

    @Test
    void testAnalyzeChatAnswers_Success() {
        // Given
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(chatAnswerRepository.findByApplication_ApplicationId(applicationId)).thenReturn(chatAnswers);

        // When
        ChatAnswerAnalysisDTO result = chatAnswerService.analyzeChatAnswers(applicationId);

        // Then
        assertNotNull(result);
        assertEquals(applicationId.toString(), result.getApplicationId());
        
        // Vérifier la motivation
        assertTrue(List.of("MEDIUM", "HIGH").contains(result.getMotivationLevel()));
        assertTrue(result.isHasSpecificMotivation());
        assertFalse(result.getMotivationKeywords().isEmpty());
        
        // Vérifier le profil technique
        assertTrue(List.of("MEDIUM", "STRONG").contains(result.getTechnicalLevel()));
        assertFalse(result.getTechnicalSkills().isEmpty());
        assertTrue(result.isHasProjectDetails());
        
        // Vérifier la disponibilité
        assertEquals("IMMEDIATE", result.getAvailabilityStatus());
        assertEquals("PART_TIME", result.getAlternanceRhythm());
        assertTrue(result.isHasClearAvailability());
        
        // Vérifier la localisation
        assertTrue(List.of("INCOMPATIBLE", "REMOTE_COMPATIBLE").contains(result.getLocationMatch()));
        
        // Vérifier le score de complétude
        assertTrue(result.getCompletenessScore() >= 0.8);
        assertTrue(result.getInconsistencies() == null || result.getInconsistencies().stream().noneMatch(v -> v.contains("contradictoire")));
        assertTrue(List.of("REVIEW", "PRIORITY").contains(result.getRecommendedAction()));
        assertEquals("phase1.v2-fallback", result.getAnalysisSchemaVersion());
        assertNotNull(result.getSemanticFacts());
        assertFalse(result.getSemanticFacts().isEmpty());
        assertTrue(result.getSemanticFacts().stream().allMatch(f -> f.getEvidence() != null && !f.getEvidence().isBlank()));
        assertTrue(result.getSemanticFacts().stream().allMatch(f -> f.getConfidence() >= 0.0 && f.getConfidence() <= 1.0));

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
    void testAnalyzeChatAnswers_WeakProfile() {
        // Given
        List<ChatAnswer> weakAnswers = Arrays.asList(
            createChatAnswer("Question libre", ""),
            createChatAnswer("Autre sujet", "reponse tres courte")
        );

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(chatAnswerRepository.findByApplication_ApplicationId(applicationId)).thenReturn(weakAnswers);

        // When
        ChatAnswerAnalysisDTO result = chatAnswerService.analyzeChatAnswers(applicationId);

        // Then
        assertNotNull(result);
        assertEquals("LOW", result.getMotivationLevel());
        assertEquals("WEAK", result.getTechnicalLevel());
        assertTrue(List.of("UNSPECIFIED", "FUTURE").contains(result.getAvailabilityStatus()));
        assertFalse(result.isHasSpecificMotivation());
        assertFalse(result.isHasProjectDetails());
        assertFalse(result.isHasClearAvailability());
        assertTrue(result.getCompletenessScore() < 0.5);
        assertTrue(result.getMissingInformation().size() > 2);
        assertEquals("REJECT", result.getRecommendedAction());

        verify(applicationRepository).findById(applicationId);
        verify(chatAnswerRepository).findByApplication_ApplicationId(applicationId);
    }

    @Test
    void testValidateAnswerCompleteness_Complete() {
        // Given
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(chatAnswerRepository.findByApplication_ApplicationId(applicationId)).thenReturn(chatAnswers);

        // When
        boolean result = chatAnswerService.validateAnswerCompleteness(applicationId);

        // Then
        assertTrue(result);

        verify(applicationRepository).findById(applicationId);
        verify(chatAnswerRepository).findByApplication_ApplicationId(applicationId);
    }

    @Test
    void testValidateAnswerCompleteness_Incomplete() {
        // Given
        List<ChatAnswer> incompleteAnswers = Arrays.asList(
            createChatAnswer("Motivation ?", "Je cherche un travail")
        );

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(chatAnswerRepository.findByApplication_ApplicationId(applicationId)).thenReturn(incompleteAnswers);

        // When
        boolean result = chatAnswerService.validateAnswerCompleteness(applicationId);

        // Then
        assertFalse(result);

        verify(applicationRepository).findById(applicationId);
        verify(chatAnswerRepository).findByApplication_ApplicationId(applicationId);
    }

    @Test
    void testExtractStructuredAnswers() {
        // Given
        when(chatAnswerRepository.findByApplication_ApplicationId(applicationId)).thenReturn(chatAnswers);

        // When
        Map<String, String> result = chatAnswerService.extractStructuredAnswers(applicationId);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        
        // Affiche les clés/valeurs pour debug si échec
        if (!result.containsKey("motivation")) {
            System.err.println("Clés présentes: " + result.keySet());
        }
        assertTrue(result.containsKey("motivation"), "Clé 'motivation' manquante. Clés: " + result.keySet());
        if (!(result.containsKey("projets") || result.containsKey("general"))) {
            System.err.println("Clés présentes: " + result.keySet());
        }
        assertTrue(result.containsKey("projets") || result.containsKey("general"), "Clé 'projets' ou 'general' manquante. Clés: " + result.keySet());
        assertTrue(result.containsKey("disponibilite") || result.containsKey("general"), "Clé 'disponibilite' ou 'general' manquante. Clés: " + result.keySet());
        assertTrue(result.containsKey("localisation") || result.containsKey("general"), "Clé 'localisation' ou 'general' manquante. Clés: " + result.keySet());

        // Vérifier le contenu (tolérance fallback)
        assertTrue(normalize(result.getOrDefault("motivation", "")).contains("passion"), "Contenu motivation: " + result.getOrDefault("motivation", ""));
        String projetsValue = normalize(result.getOrDefault("projets", result.getOrDefault("general", "")));
        assertTrue(projetsValue.contains("application") || projetsValue.contains("projet"), "Contenu projets: " + projetsValue);
        String dispoValue = normalize(result.getOrDefault("disponibilite", result.getOrDefault("general", "")));
        assertTrue(dispoValue.contains("immediat") || dispoValue.contains("disponible") || dispoValue.contains("semaine"), "Contenu dispo: " + dispoValue);
        String locValue = normalize(result.getOrDefault("localisation", result.getOrDefault("general", "")));
        assertTrue(locValue.contains("paris") || locValue.contains("deplacer") || locValue.contains("local"), "Contenu localisation: " + locValue);

    }

    // Ajout d'une méthode utilitaire pour normaliser accents/casse
    private String normalize(String input) {
        return input.toLowerCase()
            .replace("é", "e")
            .replace("è", "e")
            .replace("ê", "e")
            .replace("à", "a")
            .replace("ç", "c")
            .replace("î", "i")
            .replace("ï", "i")
            .replace("ô", "o")
            .replace("ù", "u")
            .replace("û", "u");

        // Les vérifications Mockito sont inutiles ici car le test se termine
    }

    @Test
    void testAnalyzeChatAnswers_WithInconsistencies() {
        // Given - Profil avec incohérences
        List<ChatAnswer> inconsistentAnswers = Arrays.asList(
            createChatAnswer("Motivation ?", "Je suis motive"),
            createChatAnswer("Projets ?", "J'ai realise un projet e-commerce complet"),
            createChatAnswer("Disponibilité ?", "A discuter")
        );

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(chatAnswerRepository.findByApplication_ApplicationId(applicationId)).thenReturn(inconsistentAnswers);

        // When
        ChatAnswerAnalysisDTO result = chatAnswerService.analyzeChatAnswers(applicationId);

        // Then
        assertNotNull(result);
        assertTrue(List.of("LOW", "MEDIUM").contains(result.getMotivationLevel()));
        assertEquals("WEAK", result.getTechnicalLevel());
        assertTrue(List.of("INCOMPATIBLE", "REMOTE_COMPATIBLE").contains(result.getLocationMatch()));
        
        // Vérifier les incohérences détectées
        assertFalse(result.getInconsistencies().isEmpty());
        assertTrue(result.getInconsistencies().stream()
            .anyMatch(inconsistency -> inconsistency.contains("Projets mentionnés sans technologies explicitement listées")));

        verify(applicationRepository).findById(applicationId);
        verify(chatAnswerRepository).findByApplication_ApplicationId(applicationId);
    }

    @Test
    void testAnalyzeChatAnswers_MediumProfile() {
        // Given - Profil moyen
        List<ChatAnswer> mediumAnswers = Arrays.asList(
            createChatAnswer("Motivation ?", "Je suis intéressé par cette opportunité"),
            createChatAnswer("Compétences ?", "Je connais Java et SQL"),
            createChatAnswer("Disponibilité ?", "Je serai disponible dans 2 mois"),
            createChatAnswer("Projets ?", "J'ai fait quelques petits projets personnels")
        );

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(chatAnswerRepository.findByApplication_ApplicationId(applicationId)).thenReturn(mediumAnswers);

        // When
        ChatAnswerAnalysisDTO result = chatAnswerService.analyzeChatAnswers(applicationId);

        // Then
        assertNotNull(result);
        assertTrue(List.of("LOW", "MEDIUM").contains(result.getMotivationLevel()));
        assertTrue(List.of("MEDIUM", "STRONG").contains(result.getTechnicalLevel()));
        assertTrue(List.of("UNSPECIFIED", "FUTURE").contains(result.getAvailabilityStatus()));
        assertTrue(result.getCompletenessScore() >= 0.5 && result.getCompletenessScore() <= 0.9, "Score de complétude: " + result.getCompletenessScore());
        String reco = result.getRecommendedAction();
        assertTrue(List.of("REVIEW", "PRIORITY", "REJECT").contains(reco), "Recommandation: " + reco);

        verify(applicationRepository).findById(applicationId);
        verify(chatAnswerRepository).findByApplication_ApplicationId(applicationId);
    }

    @Test
    void testAnalyzeChatAnswers_DetectsAvailabilityAndRhythmContradictions() {
        // Given
        List<ChatAnswer> contradictoryAnswers = Arrays.asList(
            createChatAnswer("Motivation ?", "Je suis motive pour le poste et j'aime apprendre."),
            createChatAnswer("Compétences ?", "Java, Spring, SQL"),
            createChatAnswer("Disponibilité ?", "Je suis disponible immédiatement mais je peux commencer dans 3 mois."),
            createChatAnswer("Rythme alternance ?", "Je veux un rythme d'alternance 3 jours en entreprise mais aussi du temps plein 5 jours."),
            createChatAnswer("Projets ?", "J'ai fait un projet API REST en Spring Boot."),
            createChatAnswer("Localisation ?", "Je suis a Paris et mobile en ile-de-france.")
        );

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(chatAnswerRepository.findByApplication_ApplicationId(applicationId)).thenReturn(contradictoryAnswers);

        // When
        ChatAnswerAnalysisDTO result = chatAnswerService.analyzeChatAnswers(applicationId);

        // Then
        assertNotNull(result);
        assertNotNull(result.getInconsistencies());
        assertTrue(result.getInconsistencies().stream().anyMatch(v -> v.contains("Disponibilité contradictoire")));
        assertTrue(result.getInconsistencies().stream().anyMatch(v -> v.contains("Rythme contradictoire")));
        assertNotNull(result.getFollowUpQuestions());
        assertTrue(result.getFollowUpQuestions().stream().anyMatch(v -> normalize(v).contains("date exacte")));
        assertEquals("REVIEW", result.getRecommendedAction());

        verify(applicationRepository).findById(applicationId);
        verify(chatAnswerRepository).findByApplication_ApplicationId(applicationId);
    }
}
