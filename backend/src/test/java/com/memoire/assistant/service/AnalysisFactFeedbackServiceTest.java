package com.memoire.assistant.service;

import com.memoire.assistant.dto.AnalysisFactFeedbackRequest;
import com.memoire.assistant.dto.AnalysisFactFeedbackResponse;
import com.memoire.assistant.model.AnalysisFactFeedback;
import com.memoire.assistant.model.Application;
import com.memoire.assistant.repository.AnalysisFactFeedbackRepository;
import com.memoire.assistant.repository.ApplicationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalysisFactFeedbackServiceTest {

    @Mock
    private AnalysisFactFeedbackRepository feedbackRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private AnalysisFactFeedbackService service;

    @Test
    @DisplayName("Devrait sauvegarder un feedback CONFIRMED")
    void saveFeedbackConfirmed() {
        UUID appId = UUID.randomUUID();
        Application application = new Application();
        application.setApplicationId(appId);

        AnalysisFactFeedbackRequest request = new AnalysisFactFeedbackRequest();
        request.setDimension("motivation");
        request.setFinding("Motivation claire");
        request.setEvidence("Je suis motive par le poste");
        request.setDecision("CONFIRMED");
        request.setReviewerComment("RAS");

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));
        when(feedbackRepository.save(any(AnalysisFactFeedback.class))).thenAnswer(invocation -> {
            AnalysisFactFeedback f = invocation.getArgument(0);
            f.setFeedbackId(UUID.randomUUID());
            f.setCreatedAt(LocalDateTime.now());
            return f;
        });

        AnalysisFactFeedbackResponse result = service.saveFeedback(appId, request);

        assertNotNull(result.getFeedbackId());
        assertEquals(appId.toString(), result.getApplicationId());
        assertEquals("CONFIRMED", result.getDecision());
        assertEquals("motivation", result.getDimension());
        assertEquals("Motivation claire", result.getFinding());

        ArgumentCaptor<AnalysisFactFeedback> captor = ArgumentCaptor.forClass(AnalysisFactFeedback.class);
        verify(feedbackRepository).save(captor.capture());
        assertEquals("CONFIRMED", captor.getValue().getDecision());
    }

    @Test
    @DisplayName("Devrait rejeter CORRECTED sans correctedFinding")
    void saveFeedbackCorrectedWithoutCorrectionShouldFail() {
        UUID appId = UUID.randomUUID();
        Application application = new Application();
        application.setApplicationId(appId);

        AnalysisFactFeedbackRequest request = new AnalysisFactFeedbackRequest();
        request.setDimension("projects");
        request.setFinding("Projet detaille");
        request.setEvidence("Projet X avec stack Y");
        request.setDecision("CORRECTED");
        request.setCorrectedFinding("  ");

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));

        RuntimeException error = assertThrows(RuntimeException.class, () -> service.saveFeedback(appId, request));
        assertTrue(error.getMessage().contains("correctedFinding"));
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait rejeter une decision invalide")
    void saveFeedbackInvalidDecisionShouldFail() {
        UUID appId = UUID.randomUUID();
        Application application = new Application();
        application.setApplicationId(appId);

        AnalysisFactFeedbackRequest request = new AnalysisFactFeedbackRequest();
        request.setDimension("projects");
        request.setFinding("Projet mentionne");
        request.setEvidence("Projet A");
        request.setDecision("MAYBE");

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));

        RuntimeException error = assertThrows(RuntimeException.class, () -> service.saveFeedback(appId, request));
        assertTrue(error.getMessage().contains("decision"));
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait rejeter REJECTED sans reviewerComment")
    void saveFeedbackRejectedWithoutCommentShouldFail() {
        UUID appId = UUID.randomUUID();
        Application application = new Application();
        application.setApplicationId(appId);

        AnalysisFactFeedbackRequest request = new AnalysisFactFeedbackRequest();
        request.setDimension("projects");
        request.setFinding("Projet non fiable");
        request.setEvidence("Preuve contestable");
        request.setDecision("REJECTED");
        request.setReviewerComment("  ");

        when(applicationRepository.findById(appId)).thenReturn(Optional.of(application));

        RuntimeException error = assertThrows(RuntimeException.class, () -> service.saveFeedback(appId, request));
        assertTrue(error.getMessage().contains("reviewerComment"));
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devrait retourner l'historique des feedbacks")
    void getFeedbackByApplication() {
        UUID appId = UUID.randomUUID();

        AnalysisFactFeedback one = new AnalysisFactFeedback();
        one.setFeedbackId(UUID.randomUUID());
        one.setApplication(new Application());
        one.getApplication().setApplicationId(appId);
        one.setDimension("motivation");
        one.setFinding("Motivation claire");
        one.setEvidence("Preuve 1");
        one.setDecision("CONFIRMED");
        one.setCreatedAt(LocalDateTime.now());

        AnalysisFactFeedback two = new AnalysisFactFeedback();
        two.setFeedbackId(UUID.randomUUID());
        two.setApplication(new Application());
        two.getApplication().setApplicationId(appId);
        two.setDimension("projects");
        two.setFinding("Projet a corriger");
        two.setEvidence("Preuve 2");
        two.setDecision("CORRECTED");
        two.setCorrectedFinding("Projet corrige");
        two.setCreatedAt(LocalDateTime.now().minusMinutes(2));

        when(feedbackRepository.findByApplication_ApplicationIdOrderByCreatedAtDesc(appId)).thenReturn(List.of(one, two));

        List<AnalysisFactFeedbackResponse> result = service.getFeedbackByApplication(appId);

        assertEquals(2, result.size());
        assertEquals("motivation", result.get(0).getDimension());
        assertEquals("projects", result.get(1).getDimension());
        assertEquals("CORRECTED", result.get(1).getDecision());
        assertEquals("Projet corrige", result.get(1).getCorrectedFinding());
    }

    @Test
    @DisplayName("Devrait retourner uniquement la derniere decision par constat")
    void getLatestFeedbackByApplication() {
        UUID appId = UUID.randomUUID();

        AnalysisFactFeedback latest = new AnalysisFactFeedback();
        latest.setFeedbackId(UUID.randomUUID());
        latest.setApplication(new Application());
        latest.getApplication().setApplicationId(appId);
        latest.setDimension("motivation");
        latest.setFinding("Motivation claire");
        latest.setEvidence("Preuve recente");
        latest.setDecision("CORRECTED");
        latest.setCorrectedFinding("Motivation tres claire");
        latest.setCreatedAt(LocalDateTime.now());

        AnalysisFactFeedback older = new AnalysisFactFeedback();
        older.setFeedbackId(UUID.randomUUID());
        older.setApplication(new Application());
        older.getApplication().setApplicationId(appId);
        older.setDimension("motivation");
        older.setFinding("Motivation claire");
        older.setEvidence("Preuve ancienne");
        older.setDecision("CONFIRMED");
        older.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        AnalysisFactFeedback secondFact = new AnalysisFactFeedback();
        secondFact.setFeedbackId(UUID.randomUUID());
        secondFact.setApplication(new Application());
        secondFact.getApplication().setApplicationId(appId);
        secondFact.setDimension("projects");
        secondFact.setFinding("Projet detaille");
        secondFact.setEvidence("Preuve projet");
        secondFact.setDecision("CONFIRMED");
        secondFact.setCreatedAt(LocalDateTime.now().minusMinutes(1));

        when(feedbackRepository.findByApplication_ApplicationIdOrderByCreatedAtDesc(appId))
            .thenReturn(List.of(latest, secondFact, older));

        List<AnalysisFactFeedbackResponse> result = service.getLatestFeedbackByApplication(appId);

        assertEquals(2, result.size());
        assertEquals("motivation", result.get(0).getDimension());
        assertEquals("CORRECTED", result.get(0).getDecision());
        assertEquals("projects", result.get(1).getDimension());
    }
}
