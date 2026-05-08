package com.memoire.assistant.repository;

import com.memoire.assistant.model.ChatAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatAnswerRepository extends JpaRepository<ChatAnswer, UUID> {
    
    /**
     * Trouve toutes les réponses pour une candidature
     */
    List<ChatAnswer> findByApplication_ApplicationId(UUID applicationId);
    
    /**
     * Trouve toutes les réponses pour une session de chat
     */
    List<ChatAnswer> findByChatSession_ChatSessionId(UUID chatSessionId);
    
    /**
     * Trouve une réponse par sa clé de question et application
     */
    ChatAnswer findByApplication_ApplicationIdAndQuestionKey(UUID applicationId, String questionKey);
    
    /**
     * Compte le nombre de réponses pour une candidature
     */
    @Query("SELECT COUNT(ca) FROM ChatAnswer ca WHERE ca.application.applicationId = :applicationId")
    int countByApplicationId(@Param("applicationId") UUID applicationId);
    
    /**
     * Trouve les réponses contenant un mot-clé spécifique
     */
    @Query("SELECT ca FROM ChatAnswer ca WHERE ca.application.applicationId = :applicationId " +
           "AND (LOWER(ca.answerText) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(ca.questionText) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<ChatAnswer> findByApplicationIdAndKeyword(@Param("applicationId") UUID applicationId, 
                                                 @Param("keyword") String keyword);
    
    /**
     * Vérifie si une candidature a des réponses complètes
     */
    @Query("SELECT CASE WHEN COUNT(ca) >= :minAnswers THEN true ELSE false END " +
           "FROM ChatAnswer ca WHERE ca.application.applicationId = :applicationId")
    boolean hasCompleteAnswers(@Param("applicationId") UUID applicationId, 
                              @Param("minAnswers") int minAnswers);

    /**
     * Charge toutes les réponses pour une liste de candidatures en une seule requête
     */
    @Query("SELECT ca FROM ChatAnswer ca WHERE ca.application.applicationId IN :applicationIds")
    List<ChatAnswer> findByApplication_ApplicationIdIn(@Param("applicationIds") List<UUID> applicationIds);
}
