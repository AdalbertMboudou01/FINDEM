package com.memoire.assistant.repository;

import com.memoire.assistant.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByApplication_ApplicationId(UUID applicationId);
    List<ChatMessage> findByCandidate_CandidateId(UUID candidateId);
    List<ChatMessage> findByApplication_ApplicationIdAndMessageType(UUID applicationId, String messageType);
    void deleteByApplication_ApplicationId(UUID applicationId);
}
