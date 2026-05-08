package com.memoire.assistant.controller;

import com.memoire.assistant.dto.PasswordResetRequest;
import com.memoire.assistant.dto.PasswordResetConfirmRequest;
import com.memoire.assistant.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/password-reset")
@Tag(name = "Password Reset", description = "API pour la réinitialisation des mots de passe")
public class PasswordResetController {
    
    @Autowired
    private PasswordResetService passwordResetService;
    
    @PostMapping("/forgot")
    @Operation(summary = "Demander une réinitialisation de mot de passe", description = "Envoie un email avec un lien de réinitialisation")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody PasswordResetRequest request) {
        try {
            // Créer le token de réinitialisation
            var resetToken = passwordResetService.createPasswordResetToken(request.getEmail());
            
            // TODO: Envoyer l'email (implémenter avec un service d'envoi d'emails)
            // Pour l'instant, on retourne le token pour le développement
            
            return ResponseEntity.ok(Map.of(
                "message", "Email de réinitialisation envoyé avec succès",
                "resetToken", resetToken.getToken(), // Pour le développement uniquement
                "status", "success"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", "Erreur lors de la demande: " + e.getMessage(),
                "status", "error"
            ));
        }
    }

    @PostMapping("/reset")
    @Operation(summary = "Réinitialiser le mot de passe", description = "Confirme la réinitialisation avec le token reçu par email")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
        try {
            // Valider que les mots de passe correspondent
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "message", "Les mots de passe ne correspondent pas",
                    "status", "error"
                ));
            }
            
            // Réinitialiser le mot de passe
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            
            return ResponseEntity.ok(Map.of(
                "message", "Mot de passe réinitialisé avec succès",
                "status", "success"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "message", "Erreur lors de la réinitialisation: " + e.getMessage(),
                "status", "error"
            ));
        }
    }

    @GetMapping("/validate/{token}")
    @Operation(summary = "Valider un token de réinitialisation", description = "Vérifie si un token est valide et non expiré")
    public ResponseEntity<Map<String, Object>> validateResetToken(@PathVariable String token) {
        try {
            boolean isValid = passwordResetService.validateToken(token);
            
            return ResponseEntity.ok(Map.of(
                "valid", isValid,
                "message", isValid ? "Token valide" : "Token invalide ou expiré"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "message", "Erreur lors de la validation: " + e.getMessage(),
                "status", "error"
            ));
        }
    }

    @PostMapping("/cleanup")
    @Operation(summary = "Nettoyer les tokens expirés", description = "Supprime les tokens de réinitialisation expirés")
    public ResponseEntity<String> cleanupExpiredTokens() {
        try {
            passwordResetService.cleanupExpiredTokens();
            return ResponseEntity.ok("Tokens expirés nettoyés avec succès");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Erreur lors du nettoyage: " + e.getMessage());
        }
    }
}
