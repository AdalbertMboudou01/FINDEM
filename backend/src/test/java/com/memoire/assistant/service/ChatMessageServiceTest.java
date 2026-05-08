package com.memoire.assistant.service;

import com.memoire.assistant.dto.ChatAnswerDTO;
import com.memoire.assistant.model.ChatMessage;
import com.memoire.assistant.model.Application;
import com.memoire.assistant.model.Candidate;
import com.memoire.assistant.repository.ChatMessageRepository;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.CandidateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ChatMessageServiceTest {
    
    @Mock
    private ChatMessageRepository chatMessageRepository;
    
    @Mock
    private ApplicationRepository applicationRepository;
    
    @Mock
    private CandidateRepository candidateRepository;
    
    @InjectMocks
    private ChatMessageService chatMessageService;
    
    @Test
    @DisplayName("Devrait sauvegarder une réponse avec succès")
    void testSaveAnswer_Success() {
        // Given
        UUID applicationId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        
        ChatAnswerDTO answerDTO = new ChatAnswerDTO();
        answerDTO.setApplicationId(applicationId);
        answerDTO.setCandidateId(candidateId);
        answerDTO.setQuestionKey("motivation");
        answerDTO.setQuestionText("Pourquoi cette offre vous intéresse ?");
        answerDTO.setAnswer("Je suis très motivé par cette opportunité");
        
        Application application = createMockApplication(applicationId);
        Candidate candidate = createMockCandidate(candidateId);
        ChatMessage expectedMessage = createMockChatMessage(application, candidate, answerDTO.getQuestionKey(), answerDTO.getAnswer());
        
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.of(candidate));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(expectedMessage);
        
        // When
        ChatMessage result = chatMessageService.saveAnswer(answerDTO);
        
        // Then
        assertNotNull(result);
        verify(applicationRepository).findById(applicationId);
        verify(candidateRepository).findById(candidateId);
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }
    
    @Test
    @DisplayName("Devrait lancer une exception si application non trouvée")
    void testSaveAnswer_ApplicationNotFound() {
        // Given
        UUID applicationId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        
        ChatAnswerDTO answerDTO = new ChatAnswerDTO();
        answerDTO.setApplicationId(applicationId);
        answerDTO.setCandidateId(candidateId);
        answerDTO.setQuestionKey("motivation");
        answerDTO.setQuestionText("Pourquoi cette offre vous intéresse ?");
        answerDTO.setAnswer("Je suis très motivé");
        
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> chatMessageService.saveAnswer(answerDTO));
        
        assertEquals("Application non trouvée", exception.getMessage());
        verify(applicationRepository).findById(applicationId);
        verify(candidateRepository, never()).findById(any());
        verify(chatMessageRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Devrait lancer une exception si candidat non trouvé")
    void testSaveAnswer_CandidateNotFound() {
        // Given
        UUID applicationId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        
        ChatAnswerDTO answerDTO = new ChatAnswerDTO();
        answerDTO.setApplicationId(applicationId);
        answerDTO.setCandidateId(candidateId);
        answerDTO.setQuestionKey("motivation");
        answerDTO.setQuestionText("Pourquoi cette offre vous intéresse ?");
        answerDTO.setAnswer("Je suis très motivé");
        
        Application application = createMockApplication(applicationId);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(candidateRepository.findById(candidateId)).thenReturn(Optional.empty());
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> chatMessageService.saveAnswer(answerDTO));
        
        assertEquals("Candidat non trouvé", exception.getMessage());
        verify(applicationRepository).findById(applicationId);
        verify(candidateRepository).findById(candidateId);
        verify(chatMessageRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Devrait sauvegarder une réponse sans IDs")
    void testSaveAnswer_NoIds() {
        // Given
        ChatAnswerDTO answerDTO = new ChatAnswerDTO();
        answerDTO.setQuestionKey("motivation");
        answerDTO.setQuestionText("Pourquoi cette offre vous intéresse ?");
        answerDTO.setAnswer("Je suis très motivé");
        
        ChatMessage expectedMessage = createMockChatMessage(null, null, answerDTO.getQuestionKey(), answerDTO.getAnswer());
        
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(expectedMessage);
        
        // When
        ChatMessage result = chatMessageService.saveAnswer(answerDTO);
        
        // Then
        assertNotNull(result);
        verify(applicationRepository, never()).findById(any());
        verify(candidateRepository, never()).findById(any());
        verify(chatMessageRepository).save(any(ChatMessage.class));
    }
    
    @Test
    @DisplayName("Devrait récupérer les messages par application")
    void testGetMessagesByApplication() {
        // Given
        UUID applicationId = UUID.randomUUID();
        List<ChatMessage> expectedMessages = Arrays.asList(
            createMockChatMessage(null, null, "motivation", "Réponse motivation"),
            createMockChatMessage(null, null, "techno", "Réponse techno")
        );
        
        when(chatMessageRepository.findByApplication_ApplicationId(applicationId))
            .thenReturn(expectedMessages);
        
        // When
        List<ChatMessage> result = chatMessageService.getMessagesByApplication(applicationId);
        
        // Then
        assertEquals(expectedMessages, result);
        verify(chatMessageRepository).findByApplication_ApplicationId(applicationId);
    }
    
    @Test
    @DisplayName("Devrait récupérer les messages par candidat")
    void testGetMessagesByCandidate() {
        // Given
        UUID candidateId = UUID.randomUUID();
        List<ChatMessage> expectedMessages = Arrays.asList(
            createMockChatMessage(null, null, "motivation", "Réponse motivation"),
            createMockChatMessage(null, null, "techno", "Réponse techno")
        );
        
        when(chatMessageRepository.findByCandidate_CandidateId(candidateId))
            .thenReturn(expectedMessages);
        
        // When
        List<ChatMessage> result = chatMessageService.getMessagesByCandidate(candidateId);
        
        // Then
        assertEquals(expectedMessages, result);
        verify(chatMessageRepository).findByCandidate_CandidateId(candidateId);
    }
    
    @Test
    @DisplayName("Devrait récupérer les réponses par application")
    void testGetAnswersByApplication() {
        // Given
        UUID applicationId = UUID.randomUUID();
        List<ChatMessage> allMessages = Arrays.asList(
            createMockChatMessage(null, null, "motivation", "Réponse motivation", "ANSWER"),
            createMockChatMessage(null, null, "techno", "Réponse techno", "ANSWER"),
            createMockChatMessage(null, null, "question", "Question posée", "QUESTION")
        );
        List<ChatMessage> expectedAnswers = Arrays.asList(allMessages.get(0), allMessages.get(1));
        
        when(chatMessageRepository.findByApplication_ApplicationIdAndMessageType(applicationId, "ANSWER"))
            .thenReturn(expectedAnswers);
        
        // When
        List<ChatMessage> result = chatMessageService.getAnswersByApplication(applicationId);
        
        // Then
        assertEquals(expectedAnswers, result);
        verify(chatMessageRepository).findByApplication_ApplicationIdAndMessageType(applicationId, "ANSWER");
    }
    
    @Test
    @DisplayName("Devrait supprimer les messages par application")
    void testDeleteMessagesByApplication() {
        // Given
        UUID applicationId = UUID.randomUUID();
        
        // When
        chatMessageService.deleteMessagesByApplication(applicationId);
        
        // Then
        verify(chatMessageRepository).deleteByApplication_ApplicationId(applicationId);
    }
    
    @Test
    @DisplayName("Devrait vérifier que toutes les réponses requises sont présentes - vrai")
    void testHasAllRequiredAnswers_True() {
        // Given
        UUID applicationId = UUID.randomUUID();
        List<String> requiredKeys = Arrays.asList("motivation", "techno", "projet");
        List<ChatMessage> answers = Arrays.asList(
            createMockChatMessage(null, null, "motivation", "Réponse motivation", "ANSWER"),
            createMockChatMessage(null, null, "techno", "Réponse techno", "ANSWER"),
            createMockChatMessage(null, null, "projet", "Réponse projet", "ANSWER")
        );
        
        when(chatMessageRepository.findByApplication_ApplicationIdAndMessageType(applicationId, "ANSWER"))
            .thenReturn(answers);
        
        // When
        boolean result = chatMessageService.hasAllRequiredAnswers(applicationId, requiredKeys);
        
        // Then
        assertTrue(result);
        verify(chatMessageRepository).findByApplication_ApplicationIdAndMessageType(applicationId, "ANSWER");
    }
    
    @Test
    @DisplayName("Devrait vérifier que toutes les réponses requises sont présentes - faux")
    void testHasAllRequiredAnswers_False() {
        // Given
        UUID applicationId = UUID.randomUUID();
        List<String> requiredKeys = Arrays.asList("motivation", "techno", "projet");
        List<ChatMessage> answers = Arrays.asList(
            createMockChatMessage(null, null, "motivation", "Réponse motivation", "ANSWER"),
            createMockChatMessage(null, null, "techno", "Réponse techno", "ANSWER")
            // Manque "projet"
        );
        
        when(chatMessageRepository.findByApplication_ApplicationIdAndMessageType(applicationId, "ANSWER"))
            .thenReturn(answers);
        
        // When
        boolean result = chatMessageService.hasAllRequiredAnswers(applicationId, requiredKeys);
        
        // Then
        assertFalse(result);
        verify(chatMessageRepository).findByApplication_ApplicationIdAndMessageType(applicationId, "ANSWER");
    }
    
    @Test
    @DisplayName("Devrait vérifier les réponses requises avec liste vide")
    void testHasAllRequiredAnswers_EmptyRequired() {
        // Given
        UUID applicationId = UUID.randomUUID();
        List<String> requiredKeys = Arrays.asList();
        List<ChatMessage> answers = Arrays.asList();
        
        when(chatMessageRepository.findByApplication_ApplicationIdAndMessageType(applicationId, "ANSWER"))
            .thenReturn(answers);
        
        // When
        boolean result = chatMessageService.hasAllRequiredAnswers(applicationId, requiredKeys);
        
        // Then
        assertTrue(result);
        verify(chatMessageRepository).findByApplication_ApplicationIdAndMessageType(applicationId, "ANSWER");
    }
    
    @Test
    @DisplayName("Devrait vérifier les réponses requises avec aucune réponse")
    void testHasAllRequiredAnswers_NoAnswers() {
        // Given
        UUID applicationId = UUID.randomUUID();
        List<String> requiredKeys = Arrays.asList("motivation", "techno");
        List<ChatMessage> answers = Arrays.asList();
        
        when(chatMessageRepository.findByApplication_ApplicationIdAndMessageType(applicationId, "ANSWER"))
            .thenReturn(answers);
        
        // When
        boolean result = chatMessageService.hasAllRequiredAnswers(applicationId, requiredKeys);
        
        // Then
        assertFalse(result);
        verify(chatMessageRepository).findByApplication_ApplicationIdAndMessageType(applicationId, "ANSWER");
    }
    
    // Méthodes utilitaires pour les tests
    private Application createMockApplication(UUID applicationId) {
        Application application = new Application();
        application.setApplicationId(applicationId);
        return application;
    }
    
    private Candidate createMockCandidate(UUID candidateId) {
        Candidate candidate = new Candidate();
        candidate.setCandidateId(candidateId);
        candidate.setFirstName("John");
        candidate.setLastName("Doe");
        candidate.setEmail("john@example.com");
        return candidate;
    }
    
    private ChatMessage createMockChatMessage(Application application, Candidate candidate, String questionKey, String answer) {
        return createMockChatMessage(application, candidate, questionKey, "Question text", answer, "ANSWER");
    }
    
    private ChatMessage createMockChatMessage(Application application, Candidate candidate, String questionKey, String questionText, String answer, String messageType) {
        ChatMessage message = new ChatMessage();
        message.setApplication(application);
        message.setCandidate(candidate);
        message.setQuestionKey(questionKey);
        message.setQuestionText(questionText);
        message.setAnswer(answer);
        message.setMessageType(messageType);
        return message;
    }
    
    private ChatMessage createMockChatMessage(Application application, Candidate candidate, String questionKey, String answer, String messageType) {
        ChatMessage message = new ChatMessage();
        message.setApplication(application);
        message.setCandidate(candidate);
        message.setQuestionKey(questionKey);
        message.setAnswer(answer);
        message.setMessageType(messageType);
        return message;
    }
}
