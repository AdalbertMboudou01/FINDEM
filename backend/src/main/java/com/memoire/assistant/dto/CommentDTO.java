package com.memoire.assistant.dto;

import com.memoire.assistant.model.Comment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CommentDTO {

    private UUID id;
    private UUID applicationId;
    private UUID companyId;
    private UUID authorId;
    private String authorType;
    private String body;
    private List<UUID> mentions;
    private String visibility;
    private UUID parentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CommentDTO from(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setApplicationId(comment.getApplication().getApplicationId());
        dto.setCompanyId(comment.getCompanyId());
        dto.setAuthorId(comment.getAuthorId());
        dto.setAuthorType(comment.getAuthorType() != null ? comment.getAuthorType().name() : null);
        dto.setBody(comment.getBody());
        dto.setMentions(comment.getMentions());
        dto.setVisibility(comment.getVisibility() != null ? comment.getVisibility().name() : "INTERNAL");
        dto.setParentId(comment.getParent() != null ? comment.getParent().getId() : null);
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());
        return dto;
    }

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getApplicationId() { return applicationId; }
    public void setApplicationId(UUID applicationId) { this.applicationId = applicationId; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public UUID getAuthorId() { return authorId; }
    public void setAuthorId(UUID authorId) { this.authorId = authorId; }

    public String getAuthorType() { return authorType; }
    public void setAuthorType(String authorType) { this.authorType = authorType; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public List<UUID> getMentions() { return mentions; }
    public void setMentions(List<UUID> mentions) { this.mentions = mentions; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
