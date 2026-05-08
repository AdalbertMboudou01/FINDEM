package com.memoire.assistant.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChatSessionDTO {
    private UUID sessionId;
    private UUID applicationId;
    private String jobTitle;
    private String companyName;
    private String candidateName;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private int progress;
    private int totalQuestions;
    private int completionScore;
    private boolean abandoned;
    private ChatQuestionDTO currentQuestion;
    
    // Constructors
    public ChatSessionDTO() {}
    
    // Getters & Setters
    public UUID getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }
    
    public UUID getApplicationId() {
        return applicationId;
    }
    
    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }
    
    public String getJobTitle() {
        return jobTitle;
    }
    
    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public String getCandidateName() {
        return candidateName;
    }
    
    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }
    
    public LocalDateTime getStartedAt() {
        return startedAt;
    }
    
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }
    
    public LocalDateTime getEndedAt() {
        return endedAt;
    }
    
    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }
    
    public int getProgress() {
        return progress;
    }
    
    public void setProgress(int progress) {
        this.progress = progress;
    }
    
    public int getTotalQuestions() {
        return totalQuestions;
    }
    
    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }
    
    public int getCompletionScore() {
        return completionScore;
    }
    
    public void setCompletionScore(int completionScore) {
        this.completionScore = completionScore;
    }
    
    public boolean isAbandoned() {
        return abandoned;
    }
    
    public void setAbandoned(boolean abandoned) {
        this.abandoned = abandoned;
    }
    
    public ChatQuestionDTO getCurrentQuestion() {
        return currentQuestion;
    }
    
    public void setCurrentQuestion(ChatQuestionDTO currentQuestion) {
        this.currentQuestion = currentQuestion;
    }
}
