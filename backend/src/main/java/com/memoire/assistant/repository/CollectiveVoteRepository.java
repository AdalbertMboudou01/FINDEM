package com.memoire.assistant.repository;

import com.memoire.assistant.model.CollectiveVote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollectiveVoteRepository extends JpaRepository<CollectiveVote, UUID> {
    Optional<CollectiveVote> findByApplicationIdAndStatus(UUID applicationId, String status);
    List<CollectiveVote> findByStatusAndClosesAtBefore(String status, LocalDateTime now);
    List<CollectiveVote> findByStatusAndReminderSentFalseAndClosesAtBefore(String status, LocalDateTime threshold);
    List<CollectiveVote> findByApplicationIdOrderByCreatedAtDesc(UUID applicationId);
}
