package com.memoire.assistant.controller;

import com.memoire.assistant.dto.CommentCreateRequest;
import com.memoire.assistant.dto.CommentDTO;
import com.memoire.assistant.model.Comment;
import com.memoire.assistant.service.CommentService;
import com.memoire.assistant.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/applications/{applicationId}/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * GET /api/applications/{applicationId}/comments
     * Liste tous les commentaires d'une candidature.
     */
    @GetMapping
    public ResponseEntity<List<CommentDTO>> listComments(@PathVariable UUID applicationId) {
        requireCompanyId();
        List<CommentDTO> result = commentService.getCommentsForApplication(applicationId)
                .stream()
                .map(CommentDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/applications/{applicationId}/comments
     * Crée un commentaire. Les @mentions détectées dans le body déclenchent des notifications.
     */
    @PostMapping
    public ResponseEntity<CommentDTO> createComment(
            @PathVariable UUID applicationId,
            @RequestBody CommentCreateRequest request) {
        requireCompanyId();

        if (request.getBody() == null || request.getBody().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le corps du commentaire est obligatoire");
        }

        Comment saved = commentService.createComment(
                applicationId,
                request.getBody(),
                request.getMentions(),
                request.getVisibility(),
                request.getParentId());

        return ResponseEntity.status(HttpStatus.CREATED).body(CommentDTO.from(saved));
    }

    /**
     * DELETE /api/applications/{applicationId}/comments/{commentId}
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable UUID applicationId,
            @PathVariable UUID commentId) {
        requireCompanyId();
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    private void requireCompanyId() {
        if (TenantContext.getCompanyId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Contexte entreprise manquant");
        }
    }
}
