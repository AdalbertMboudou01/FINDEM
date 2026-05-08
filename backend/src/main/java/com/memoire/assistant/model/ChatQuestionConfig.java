package com.memoire.assistant.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Column;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_question_configs")
public class ChatQuestionConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID configId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;
    
    @Column(name = "question_key", nullable = false)
    private String questionKey;
    
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;
    
    @Column(name = "question_type", nullable = false)
    private String questionType; // OPEN, MULTIPLE_CHOICE, RATING, BOOLEAN
    
    @Column(name = "required", nullable = false)
    private Boolean required = false;
    
    @Column(name = "order_index")
    private Integer orderIndex;
    
    @Column(name = "adaptive_enabled", nullable = false)
    private Boolean adaptiveEnabled = false; // Pour l'IA adaptative
    
    @Column(name = "follow_up_questions", columnDefinition = "TEXT")
    private String followUpQuestions; // JSON avec questions de suivi
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Constructeurs
    public ChatQuestionConfig() {
        this.createdAt = LocalDateTime.now();
    }
    
    public ChatQuestionConfig(Job job, String questionKey, String questionText, String questionType) {
        this();
        this.job = job;
        this.questionKey = questionKey;
        this.questionText = questionText;
        this.questionType = questionType;
    }
    
    // Getters & Setters
    public UUID getConfigId() {
        return configId;
    }
    
    public void setConfigId(UUID configId) {
        this.configId = configId;
    }
    
    public Job getJob() {
        return job;
    }
    
    public void setJob(Job job) {
        this.job = job;
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
    
    public String getQuestionType() {
        return questionType;
    }
    
    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }
    
    public Boolean getRequired() {
        return required;
    }
    
    public void setRequired(Boolean required) {
        this.required = required;
    }
    
    public Integer getOrderIndex() {
        return orderIndex;
    }
    
    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
    
    public Boolean getAdaptiveEnabled() {
        return adaptiveEnabled;
    }
    
    public void setAdaptiveEnabled(Boolean adaptiveEnabled) {
        this.adaptiveEnabled = adaptiveEnabled;
    }
    
    public String getFollowUpQuestions() {
        return followUpQuestions;
    }
    
    public void setFollowUpQuestions(String followUpQuestions) {
        this.followUpQuestions = followUpQuestions;
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
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
