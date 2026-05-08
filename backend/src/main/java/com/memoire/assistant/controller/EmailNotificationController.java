package com.memoire.assistant.controller;

import com.memoire.assistant.dto.EmailNotificationDTO;
import com.memoire.assistant.service.EmailNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Email Notifications", description = "API pour la gestion des notifications email")
public class EmailNotificationController {
    
    @Autowired
    private EmailNotificationService emailNotificationService;
    
    @PostMapping("/send")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Envoyer une notification email", description = "Envoie une notification email personnalisée")
    public ResponseEntity<Map<String, Object>> sendEmail(@RequestBody EmailNotificationDTO notification) {
        try {
            boolean sent = emailNotificationService.sendEmail(notification);
            
            return ResponseEntity.ok(Map.of(
                "success", sent,
                "message", sent ? "Email envoyé avec succès" : "Échec de l'envoi",
                "notification", notification
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Erreur lors de l'envoi de l'email: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/candidate/retention/{applicationId}")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Notifier un candidat retenu", description = "Envoie un email de rétention au candidat")
    public ResponseEntity<Map<String, Object>> sendCandidateRetentionEmail(@PathVariable UUID applicationId) {
        try {
            EmailNotificationDTO notification = emailNotificationService.sendCandidateRetentionEmail(applicationId);
            
            return ResponseEntity.ok(Map.of(
                "success", notification.isSent(),
                "message", notification.isSent() ? "Email de rétention envoyé" : "Échec de l'envoi",
                "candidateEmail", notification.getTo(),
                "candidateName", notification.getCandidateName(),
                "jobTitle", notification.getJobTitle(),
                "sentAt", notification.getSentAt(),
                "errorMessage", notification.getErrorMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Erreur lors de l'envoi de l'email: " + e.getMessage()
            ));
        }
    }
    
    @PostMapping("/candidate/rejection/{applicationId}")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Notifier un candidat non retenu", description = "Envoie un email de rejet au candidat")
    public ResponseEntity<Map<String, Object>> sendCandidateRejectionEmail(@PathVariable UUID applicationId) {
        try {
            EmailNotificationDTO notification = emailNotificationService.sendCandidateRejectionEmail(applicationId);
            
            return ResponseEntity.ok(Map.of(
                "success", notification.isSent(),
                "message", notification.isSent() ? "Email de rejet envoyé" : "Échec de l'envoi",
                "candidateEmail", notification.getTo(),
                "candidateName", notification.getCandidateName(),
                "jobTitle", notification.getJobTitle(),
                "sentAt", notification.getSentAt(),
                "errorMessage", notification.getErrorMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Erreur lors de l'envoi de l'email: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/templates")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Lister les templates d'emails disponibles", description = "Retourne la liste des types de notifications")
    public ResponseEntity<Map<String, Object>> getAvailableTemplates() {
        return ResponseEntity.ok(Map.of(
            "templates", List.of(
                "CANDIDATE_POOL",
                "CANDIDATE_ACKNOWLEDGMENT", 
                "INTERVIEW_REMINDER",
                "INTERVIEW_CONFIRMATION",
                "RECRUITER_SUMMARY",
                "CANDIDATE_RETENTION",
                "CANDIDATE_REJECTION"
            ),
            "message", "Templates d'emails disponibles"
        ));
    }
    
    @GetMapping("/status/{applicationId}")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('MANAGER') or hasRole('ADMIN')")
    @Operation(summary = "Vérifier le statut des notifications", description = "Vérifie si des emails ont été envoyés pour une candidature")
    public ResponseEntity<Map<String, Object>> getNotificationStatus(@PathVariable UUID applicationId) {
        // Simulation de vérification de statut
        return ResponseEntity.ok(Map.of(
            "applicationId", applicationId,
            "emailsSent", List.of(
                Map.of("type", "ACKNOWLEDGMENT", "sent", true, "sentAt", LocalDateTime.now().minusDays(1)),
                Map.of("type", "SCREENING", "sent", true, "sentAt", LocalDateTime.now().minusHours(2))
            ),
            "message", "Statut des notifications récupéré"
        ));
    }
}
