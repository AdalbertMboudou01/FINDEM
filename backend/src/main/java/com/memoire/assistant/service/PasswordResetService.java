package com.memoire.assistant.service;

import com.memoire.assistant.model.PasswordResetToken;
import com.memoire.assistant.model.User;
import com.memoire.assistant.repository.PasswordResetTokenRepository;
import com.memoire.assistant.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.security.SecureRandom;

@Service
public class PasswordResetService {
    
    @Autowired
    private PasswordResetTokenRepository tokenRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private static final int TOKEN_EXPIRATION_HOURS = 24;
    private static final int TOKEN_LENGTH = 32;
    
    /**
     * Générer un token sécurisé
     */
    public String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[TOKEN_LENGTH];
        random.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().encodeToString(bytes);
    }
    
    /**
     * Créer une demande de réinitialisation
     */
    public PasswordResetToken createPasswordResetToken(String email) {
        // Vérifier si l'utilisateur existe
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Utilisateur non trouvé pour l'email: " + email);
        }
        
        // Nettoyer les anciens tokens expirés
        cleanupExpiredTokens();
        
        // Créer le nouveau token
        String tokenValue = generateSecureToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRATION_HOURS);
        
        PasswordResetToken token = new PasswordResetToken(userOpt.get(), tokenValue, expiresAt);
        return tokenRepository.save(token);
    }
    
    /**
     * Valider un token
     */
    public boolean validateToken(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return false;
        }
        
        PasswordResetToken resetToken = tokenOpt.get();
        
        // Vérifier si le token est expiré
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        
        // Vérifier si le token est déjà utilisé
        if (resetToken.getUsed()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Réinitialiser le mot de passe
     */
    public void resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Token invalide");
        }
        
        PasswordResetToken resetToken = tokenOpt.get();
        
        // Valider à nouveau le token
        if (!validateToken(token)) {
            throw new IllegalArgumentException("Token expiré ou déjà utilisé");
        }
        
        // Mettre à jour le mot de passe de l'utilisateur
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        // Marquer le token comme utilisé
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
    
    /**
     * Nettoyer les tokens expirés
     */
    public void cleanupExpiredTokens() {
        List<PasswordResetToken> expiredTokens = tokenRepository.findExpiredTokens(LocalDateTime.now());
        for (PasswordResetToken token : expiredTokens) {
            token.setUsed(true);
            tokenRepository.save(token);
        }
    }
    
    /**
     * Compter les tokens actifs pour un utilisateur
     */
    public long countActiveTokens(UUID userId) {
        return tokenRepository.countActiveTokensByUser(userId, LocalDateTime.now());
    }
}
