package com.memoire.assistant.repository;

import com.memoire.assistant.model.FinalVote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinalVoteRepository extends JpaRepository<FinalVote, UUID> {
    List<FinalVote> findByApplicationId(UUID applicationId);
    Optional<FinalVote> findByApplicationIdAndVoterRole(UUID applicationId, String voterRole);
    boolean existsByApplicationIdAndVoterId(UUID applicationId, UUID voterId);
    long countByApplicationId(UUID applicationId);
}
