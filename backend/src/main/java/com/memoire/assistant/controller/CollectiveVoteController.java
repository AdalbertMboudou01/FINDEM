package com.memoire.assistant.controller;

import com.memoire.assistant.service.CollectiveVoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications/{applicationId}/collective-vote")
public class CollectiveVoteController {

    @Autowired
    private CollectiveVoteService collectiveVoteService;

    /** État du vote courant pour une candidature */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getVoteStatus(@PathVariable UUID applicationId) {
        return ResponseEntity.ok(collectiveVoteService.getVoteStatus(applicationId));
    }

    /** Soumettre son bulletin de vote anonyme */
    @PostMapping("/{voteId}/ballot")
    public ResponseEntity<?> castBallot(
            @PathVariable UUID applicationId,
            @PathVariable UUID voteId,
            @RequestBody Map<String, String> body) {
        try {
            String choice = body.get("choice"); // APPROVE | REJECT
            return ResponseEntity.ok(collectiveVoteService.castBallot(voteId, choice));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
