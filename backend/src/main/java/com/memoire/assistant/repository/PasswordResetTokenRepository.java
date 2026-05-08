package com.memoire.assistant.repository;

import com.memoire.assistant.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    
    /**
     * Trouver un token par sa valeur
     */
    Optional<PasswordResetToken> findByToken(String token);
    
    /**
     * Trouver un token par utilisateur et non utilisé
     */
    Optional<PasswordResetToken> findByUser_IdAndUsedFalse(UUID userId);
    
    /**
     * Trouver tous les tokens expirés
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.expiresAt < :now AND t.used = false")
    java.util.List<PasswordResetToken> findExpiredTokens(@Param("now") LocalDateTime now);
    
    /**
     * Supprimer les tokens expirés
     */
    void deleteByExpiresAtBefore(LocalDateTime dateTime);
    
    /**
     * Compter les tokens actifs pour un utilisateur
     */
    @Query("SELECT COUNT(t) FROM PasswordResetToken t WHERE t.user.id = :userId AND t.used = false AND t.expiresAt > :now")
    long countActiveTokensByUser(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}
