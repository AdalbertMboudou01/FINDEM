package com.memoire.assistant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

public class EmailNotificationDTO {
    
    @NotBlank(message = "L'email du destinataire est obligatoire")
    @Email(message = "L'email doit être valide")
    private String to;
    
    @NotBlank(message = "L'objet de l'email est obligatoire")
    private String subject;
    
    @NotBlank(message = "Le contenu de l'email est obligatoire")
    private String content;
    
    private String templateName;
    private Map<String, Object> templateVariables;
    
    @NotNull(message = "Le type de notification est obligatoire")
    private NotificationType type;
    
    private String applicationId;
    private String candidateName;
    private String jobTitle;
    private String companyName;
    
    private LocalDateTime scheduledAt;
    private boolean sent;
    private LocalDateTime sentAt;
    private String errorMessage;
    
    // Types de notifications
    public enum NotificationType {
        CANDIDATE_RETENTION,      // Candidat retenu pour entretien
        CANDIDATE_REJECTION,      // Candidat non retenu
        CANDIDATE_POOL,           // Candidat mis au vivier
        CANDIDATE_ACKNOWLEDGMENT, // Accusé réception candidature
        INTERVIEW_REMINDER,       // Rappel entretien
        INTERVIEW_CONFIRMATION,   // Confirmation entretien
        RECRUITER_SUMMARY         // Résumé quotidien recruteur
    }
    
    // Constructeurs
    public EmailNotificationDTO() {}
    
    public EmailNotificationDTO(String to, String subject, String content, NotificationType type) {
        this.to = to;
        this.subject = subject;
        this.content = content;
        this.type = type;
        this.scheduledAt = LocalDateTime.now();
        this.sent = false;
    }
    
    // Getters & Setters
    public String getTo() {
        return to;
    }
    
    public void setTo(String to) {
        this.to = to;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getTemplateName() {
        return templateName;
    }
    
    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }
    
    public Map<String, Object> getTemplateVariables() {
        return templateVariables;
    }
    
    public void setTemplateVariables(Map<String, Object> templateVariables) {
        this.templateVariables = templateVariables;
    }
    
    public NotificationType getType() {
        return type;
    }
    
    public void setType(NotificationType type) {
        this.type = type;
    }
    
    public String getApplicationId() {
        return applicationId;
    }
    
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }
    
    public String getCandidateName() {
        return candidateName;
    }
    
    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
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
    
    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }
    
    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }
    
    public boolean isSent() {
        return sent;
    }
    
    public void setSent(boolean sent) {
        this.sent = sent;
    }
    
    public LocalDateTime getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
