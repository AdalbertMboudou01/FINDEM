package com.memoire.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class ChatAnswerDTO {
    @NotBlank(message = "La clé de la question est obligatoire")
    private String questionKey;
    
    @NotBlank(message = "Le texte de la question est obligatoire")
    private String questionText;
    
    @NotBlank(message = "La réponse est obligatoire")
    private String answer;
    
    private UUID answerId;
    private UUID sessionId;
    private String normalizedValue;
    private java.time.LocalDateTime answeredAt;
    private Boolean required;
    
    private UUID applicationId;
    private UUID candidateId;

    // Constructors
    public ChatAnswerDTO() {}

    public ChatAnswerDTO(String questionKey, String questionText, String answer, UUID applicationId, UUID candidateId) {
        this.questionKey = questionKey;
        this.questionText = questionText;
        this.answer = answer;
        this.applicationId = applicationId;
        this.candidateId = candidateId;
    }

    // Getters & Setters
    public String getQuestionKey() {
        return questionKey;
    }

    public void setQuestionKey(String questionKey) {
        this.questionKey = questionKey;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
    
    public UUID getAnswerId() {
        return answerId;
    }
    
    public void setAnswerId(UUID answerId) {
        this.answerId = answerId;
    }
    
    public UUID getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getNormalizedValue() {
        return normalizedValue;
    }
    
    public void setNormalizedValue(String normalizedValue) {
        this.normalizedValue = normalizedValue;
    }
    
    public java.time.LocalDateTime getAnsweredAt() {
        return answeredAt;
    }
    
    public void setAnsweredAt(java.time.LocalDateTime answeredAt) {
        this.answeredAt = answeredAt;
    }
    
    public Boolean getRequired() {
        return required;
    }
    
    public void setRequired(Boolean required) {
        this.required = required;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public UUID getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(UUID candidateId) {
        this.candidateId = candidateId;
    }
}
