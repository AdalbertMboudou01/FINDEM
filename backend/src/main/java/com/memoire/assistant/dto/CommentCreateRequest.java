package com.memoire.assistant.dto;

import java.util.List;
import java.util.UUID;

public class CommentCreateRequest {

    private String body;
    private List<UUID> mentions;
    private String visibility = "INTERNAL";
    private UUID parentId;

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public List<UUID> getMentions() { return mentions; }
    public void setMentions(List<UUID> mentions) { this.mentions = mentions; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public UUID getParentId() { return parentId; }
    public void setParentId(UUID parentId) { this.parentId = parentId; }
}
