package com.memoire.assistant.dto;

import com.memoire.assistant.model.ApplicationActivity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class ApplicationActivityDTO {

    private UUID id;
    private UUID applicationId;
    private UUID companyId;
    private UUID actorId;
    private String actorType;
    private String eventType;
    private Map<String, Object> payload;
    private String visibility;
    private LocalDateTime createdAt;

    public static ApplicationActivityDTO from(ApplicationActivity activity) {
        ApplicationActivityDTO dto = new ApplicationActivityDTO();
        dto.setId(activity.getId());
        dto.setApplicationId(activity.getApplication().getApplicationId());
        dto.setCompanyId(activity.getCompanyId());
        dto.setActorId(activity.getActorId());
        dto.setActorType(activity.getActorType() != null ? activity.getActorType().name() : null);
        dto.setEventType(activity.getEventType() != null ? activity.getEventType().name() : null);
        dto.setPayload(activity.getPayload());
        dto.setVisibility(activity.getVisibility());
        dto.setCreatedAt(activity.getCreatedAt());
        return dto;
    }

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getApplicationId() { return applicationId; }
    public void setApplicationId(UUID applicationId) { this.applicationId = applicationId; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }

    public String getActorType() { return actorType; }
    public void setActorType(String actorType) { this.actorType = actorType; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
