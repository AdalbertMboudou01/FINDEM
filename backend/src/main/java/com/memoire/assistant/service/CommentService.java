package com.memoire.assistant.service;

import com.memoire.assistant.model.ApplicationActivity.EventType;
import com.memoire.assistant.model.Comment;
import com.memoire.assistant.model.Comment.AuthorType;
import com.memoire.assistant.model.Comment.Visibility;
import com.memoire.assistant.model.InternalNotification;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.CommentRepository;
import com.memoire.assistant.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CommentService {

    /** Regex qui détecte les mentions @<uuid> dans le corps du commentaire */
    private static final Pattern MENTION_PATTERN =
            Pattern.compile("@([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})");

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationActivityService activityService;

    @Autowired
    private InternalNotificationService notificationService;

    @Autowired
    private AICommentInsightService aiCommentInsightService;

    public List<Comment> getCommentsForApplication(UUID applicationId) {
        UUID companyId = TenantContext.getCompanyId();
        return commentRepository
                .findByApplication_ApplicationIdAndCompanyIdOrderByCreatedAtAsc(applicationId, companyId);
    }

    public Comment createComment(UUID applicationId, String body, List<UUID> explicitMentions,
                                 String visibilityStr, UUID parentId) {
        UUID companyId = TenantContext.getCompanyId();

        var application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable: " + applicationId));

        UUID authorId = TenantContext.getRecruiterId() != null
                ? TenantContext.getRecruiterId()
                : TenantContext.getUserId();
        AuthorType authorType = TenantContext.getRecruiterId() != null ? AuthorType.RECRUITER : AuthorType.USER;
        Visibility visibility = parseVisibility(visibilityStr);

        // Fusion des mentions explicites + celles extraites du texte (@uuid)
        List<UUID> allMentions = new ArrayList<>(explicitMentions != null ? explicitMentions : List.of());
        extractMentionsFromBody(body).forEach(id -> {
            if (!allMentions.contains(id)) allMentions.add(id);
        });

        Comment comment = new Comment();
        comment.setApplication(application);
        comment.setCompanyId(companyId);
        comment.setAuthorId(authorId);
        comment.setAuthorType(authorType);
        comment.setBody(body);
        comment.setMentions(allMentions);
        comment.setVisibility(visibility);

        if (parentId != null) {
            commentRepository.findById(parentId).ifPresent(comment::setParent);
        }

        Comment saved = commentRepository.save(comment);

        // Événement timeline
        activityService.logEvent(applicationId, EventType.COMMENT_ADDED,
                Map.of("preview", body.length() > 80 ? body.substring(0, 80) + "…" : body,
                       "visibility", visibility.name(),
                       "mentions_count", allMentions.size()));

        // Notifications MENTION pour chaque utilisateur mentionné
        UUID companyIdFinal = companyId;
        UUID applicationIdFinal = applicationId;
        allMentions.forEach(mentionedId -> notificationService.notify(
                mentionedId, companyIdFinal,
                InternalNotification.Type.MENTION,
                "Vous avez été mentionné dans un commentaire",
                body.length() > 120 ? body.substring(0, 120) + "…" : body,
                "candidate", applicationIdFinal
        ));

        if (saved.getAuthorType() != Comment.AuthorType.AI_SYSTEM) {
            aiCommentInsightService.analyzeAndInsert(applicationId, companyId, body);
        }

        return saved;
    }

    public void deleteComment(UUID commentId) {
        UUID companyId = TenantContext.getCompanyId();
        commentRepository.findById(commentId).ifPresent(c -> {
            if (c.getCompanyId().equals(companyId)) {
                commentRepository.deleteById(commentId);
            }
        });
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private List<UUID> extractMentionsFromBody(String body) {
        List<UUID> found = new ArrayList<>();
        if (body == null) return found;
        Matcher m = MENTION_PATTERN.matcher(body);
        while (m.find()) {
            try {
                found.add(UUID.fromString(m.group(1)));
            } catch (IllegalArgumentException ignored) {}
        }
        return found;
    }

    private Visibility parseVisibility(String value) {
        if ("SHARED".equalsIgnoreCase(value)) return Visibility.SHARED;
        return Visibility.INTERNAL;
    }
}
