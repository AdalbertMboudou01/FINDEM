package com.memoire.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class AnnouncementCreateRequest {
    
    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 200, message = "Le titre ne doit pas dépasser 200 caractères")
    private String title;
    
    @NotBlank(message = "Le contenu est obligatoire")
    @Size(max = 5000, message = "Le contenu ne doit pas dépasser 5000 caractères")
    private String content;
    
    @NotBlank(message = "La catégorie est obligatoire")
    private String category; // GENERAL, HIRING, EVENT, POLICY, UPDATE
    
    @NotBlank(message = "La priorité est obligatoire")
    private String priority; // LOW, MEDIUM, HIGH, URGENT
    
    @NotBlank(message = "L'audience est obligatoire")
    private String targetAudience; // ALL, RECRUITERS, TECHNICAL, MANAGEMENT
    
    private LocalDateTime expiresAt;
    
    @Size(max = 500, message = "Le chemin du fichier ne doit pas dépasser 500 caractères")
    private String attachmentPath;
    
    @Size(max = 255, message = "Le nom du fichier ne doit pas dépasser 255 caractères")
    private String attachmentName;
    
    // Constructors
    public AnnouncementCreateRequest() {}
    
    // Getters & Setters
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public String getTargetAudience() {
        return targetAudience;
    }
    
    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public String getAttachmentPath() {
        return attachmentPath;
    }
    
    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }
    
    public String getAttachmentName() {
        return attachmentName;
    }
    
    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }
}
