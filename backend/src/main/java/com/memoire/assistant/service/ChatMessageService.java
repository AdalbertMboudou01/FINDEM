package com.memoire.assistant.service;

import com.memoire.assistant.dto.ChatAnswerDTO;
import com.memoire.assistant.model.ChatMessage;
import com.memoire.assistant.model.Application;
import com.memoire.assistant.model.Candidate;
import com.memoire.assistant.repository.ChatMessageRepository;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.CandidateRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.UUID;

@Service
public class ChatMessageService {
    
    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private ApplicationRepository applicationRepository;
    
    @Autowired
    private CandidateRepository candidateRepository;
    
    public ChatMessage saveAnswer(ChatAnswerDTO answerDTO) {
        if (answerDTO.getQuestionKey() == null || answerDTO.getQuestionKey().isBlank()) {
            throw new IllegalArgumentException("La cle de la question est obligatoire");
        }
        if (answerDTO.getQuestionText() == null || answerDTO.getQuestionText().isBlank()) {
            throw new IllegalArgumentException("Le texte de la question est obligatoire");
        }
        if (answerDTO.getAnswer() == null || answerDTO.getAnswer().isBlank()) {
            throw new IllegalArgumentException("La reponse est obligatoire");
        }

        // Vérifier que l'application et le candidat existent
        Application application = null;
        if (answerDTO.getApplicationId() != null) {
            application = applicationRepository.findById(answerDTO.getApplicationId())
                .orElseThrow(() -> new RuntimeException("Application non trouvée"));
        }
        
        Candidate candidate = null;
        if (answerDTO.getCandidateId() != null) {
            candidate = candidateRepository.findById(answerDTO.getCandidateId())
                .orElseThrow(() -> new RuntimeException("Candidat non trouvé"));
        }
        
        // Créer et sauvegarder le message
        ChatMessage message = new ChatMessage(
            application,
            candidate,
            answerDTO.getQuestionKey().trim(),
            answerDTO.getQuestionText().trim(),
            answerDTO.getAnswer().trim(),
            "ANSWER"
        );

        try {
            return chatMessageRepository.save(message);
        } catch (DataIntegrityViolationException ex) {
            String dbMessage = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : "";
            if (dbMessage != null && dbMessage.contains("character varying(255)")) {
                message.setQuestionText(truncate(message.getQuestionText(), 255));
                message.setAnswer(truncate(message.getAnswer(), 255));
                return chatMessageRepository.save(message);
            }
            throw ex;
        }
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }
    
    public List<ChatMessage> getMessagesByApplication(UUID applicationId) {
        return chatMessageRepository.findByApplication_ApplicationId(applicationId);
    }
    
    public List<ChatMessage> getMessagesByCandidate(UUID candidateId) {
        return chatMessageRepository.findByCandidate_CandidateId(candidateId);
    }
    
    public List<ChatMessage> getAnswersByApplication(UUID applicationId) {
        return chatMessageRepository.findByApplication_ApplicationIdAndMessageType(applicationId, "ANSWER");
    }
    
    public void deleteMessagesByApplication(UUID applicationId) {
        chatMessageRepository.deleteByApplication_ApplicationId(applicationId);
    }
    
    public boolean hasAllRequiredAnswers(UUID applicationId, List<String> requiredQuestionKeys) {
        List<ChatMessage> answers = getAnswersByApplication(applicationId);
        
        return requiredQuestionKeys.stream().allMatch(requiredKey -> 
            answers.stream().anyMatch(answer -> requiredKey.equals(answer.getQuestionKey()))
        );
    }
}
