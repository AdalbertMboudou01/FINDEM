package com.memoire.assistant.service;

import com.memoire.assistant.model.ApplicationActivity.EventType;
import com.memoire.assistant.model.Comment;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AIPersonaCommentService {

    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private ApplicationActivityService activityService;

    public Comment postInternalComment(UUID applicationId, UUID companyId, UUID personaId,
                                       String body, Map<String, Object> extraPayload) {
        var application = applicationRepository.findById(applicationId).orElse(null);
        if (application == null || body == null || body.isBlank()) {
            return null;
        }

        Comment aiComment = new Comment();
        aiComment.setApplication(application);
        aiComment.setCompanyId(companyId);
        aiComment.setAuthorId(personaId);
        aiComment.setAuthorType(Comment.AuthorType.AI_SYSTEM);
        aiComment.setBody(body.strip());
        aiComment.setMentions(Collections.emptyList());
        aiComment.setVisibility(Comment.Visibility.INTERNAL);
        Comment saved = commentRepository.save(aiComment);

        Map<String, Object> payload = new HashMap<>();
        payload.put("preview", preview(body));
        payload.put("visibility", "INTERNAL");
        payload.put("mentions_count", 0);
        payload.put("author_type", "AI_SYSTEM");
        if (extraPayload != null) {
            payload.putAll(extraPayload);
        }

        activityService.logSystemEvent(applicationId, companyId, EventType.COMMENT_ADDED, payload);
        return saved;
    }

    private String preview(String body) {
        return body.length() > 80 ? body.substring(0, 80) + "..." : body;
    }
}
