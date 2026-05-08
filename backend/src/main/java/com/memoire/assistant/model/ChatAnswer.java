package com.memoire.assistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_answers")
public class ChatAnswer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID answerId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id")
    private ChatSession chatSession;
    
    @Column(name = "question_key", nullable = false)
    private String questionKey;
    
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;
    
    @Column(name = "answer_text", nullable = false, columnDefinition = "TEXT")
    private String answerText;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "normalized_value")
    private String normalizedValue;
    
    @Column(name = "answered_at")
    private LocalDateTime answeredAt;
    
    @Column(name = "required", nullable = false)
    private Boolean required;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructeurs
    public ChatAnswer() {
        this.createdAt = LocalDateTime.now();
    }
    
    public ChatAnswer(Application application, String questionKey, String questionText, String answerText) {
        this();
        this.application = application;
        this.questionKey = questionKey;
        this.questionText = questionText;
        this.answerText = answerText;
    }
    
    public ChatAnswer(ChatSession chatSession, String questionKey, String questionText, String answerText, Boolean required) {
        this();
        this.chatSession = chatSession;
        this.questionKey = questionKey;
        this.questionText = questionText;
        this.answerText = answerText;
        this.required = required;
        this.answeredAt = LocalDateTime.now();
    }
    
    // Getters & Setters
    public UUID getAnswerId() {
        return answerId;
    }
    
    public void setAnswerId(UUID answerId) {
        this.answerId = answerId;
    }
    
    public Application getApplication() {
        return application;
    }
    
    public void setApplication(Application application) {
        this.application = application;
    }
    
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
    
    public String getAnswerText() {
        return answerText;
    }
    
    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public ChatSession getChatSession() {
        return chatSession;
    }
    
    public void setChatSession(ChatSession chatSession) {
        this.chatSession = chatSession;
    }
    
    public String getNormalizedValue() {
        return normalizedValue;
    }
    
    public void setNormalizedValue(String normalizedValue) {
        this.normalizedValue = normalizedValue;
    }
    
    public LocalDateTime getAnsweredAt() {
        return answeredAt;
    }
    
    public void setAnsweredAt(LocalDateTime answeredAt) {
        this.answeredAt = answeredAt;
    }
    
    public Boolean getRequired() {
        return required;
    }
    
    public void setRequired(Boolean required) {
        this.required = required;
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
