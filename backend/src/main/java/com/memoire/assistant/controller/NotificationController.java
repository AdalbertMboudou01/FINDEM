package com.memoire.assistant.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.memoire.assistant.service.NotificationService;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Gestion des notifications email")
public class NotificationController {
    
    @Autowired
    private NotificationService notificationService;
    
    @PostMapping("/application/{applicationId}/status")
    @Operation(summary = "Notifier changement de statut", description = "Envoie un email au candidat pour informer du changement de statut de sa candidature")
    public ResponseEntity<String> sendStatusUpdate(
            @PathVariable UUID applicationId,
            @RequestParam String newStatus) {
        
        try {
            notificationService.sendApplicationStatusUpdate(applicationId, newStatus);
            return ResponseEntity.ok("Email de mise à jour envoyé avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Erreur lors de l'envoi: " + e.getMessage());
        }
    }
    
    @PostMapping("/application/{applicationId}/interview")
    @Operation(summary = "Envoyer invitation entretien", description = "Envoie un email d'invitation à un entretien")
    public ResponseEntity<String> sendInterviewInvitation(
            @PathVariable UUID applicationId,
            @RequestParam String interviewDetails) {
        
        try {
            notificationService.sendInterviewInvitation(applicationId, interviewDetails);
            return ResponseEntity.ok("Email d'invitation envoyé avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Erreur lors de l'envoi: " + e.getMessage());
        }
    }
    
    @PostMapping("/application/{applicationId}/rejection")
    @Operation(summary = "Notifier rejet", description = "Envoie un email de rejet au candidat")
    public ResponseEntity<String> sendRejectionNotification(@PathVariable UUID applicationId) {
        try {
            notificationService.sendRejectionNotification(applicationId);
            return ResponseEntity.ok("Email de rejet envoyé avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Erreur lors de l'envoi: " + e.getMessage());
        }
    }
    
    @PostMapping("/application/{applicationId}/confirmation")
    @Operation(summary = "Confirmer réception candidature", description = "Envoie un email de confirmation de réception de candidature")
    public ResponseEntity<String> sendApplicationConfirmation(@PathVariable UUID applicationId) {
        try {
            notificationService.sendApplicationConfirmation(applicationId);
            return ResponseEntity.ok("Email de confirmation envoyé avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Erreur lors de l'envoi: " + e.getMessage());
        }
    }
    
    @PostMapping("/pool/{candidateId}")
    @Operation(summary = "Notifier vivier", description = "Envoie un email à un candidat du vivier pour une nouvelle opportunité")
    public ResponseEntity<String> sendPoolNotification(
            @PathVariable UUID candidateId,
            @RequestParam String jobTitle) {
        
        try {
            notificationService.sendPoolNotification(candidateId, jobTitle);
            return ResponseEntity.ok("Email de notification du vivier envoyé avec succès");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Erreur lors de l'envoi: " + e.getMessage());
        }
    }
    
    @GetMapping("/config")
    @Operation(summary = "Configuration email", description = "Retourne la configuration actuelle des emails (sans les mots de passe)")
    public ResponseEntity<?> getEmailConfig() {
        return ResponseEntity.ok(java.util.Map.of(
            "host", "smtp.gmail.com",
            "port", 587,
            "from", "noreply@assistant-prequalification.com",
            "status", "Configuré (requiert username/password dans .env)"
        ));
    }
}
