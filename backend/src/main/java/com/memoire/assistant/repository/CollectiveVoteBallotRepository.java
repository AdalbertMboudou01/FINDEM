package com.memoire.assistant.repository;

import com.memoire.assistant.model.CollectiveVoteBallot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollectiveVoteBallotRepository extends JpaRepository<CollectiveVoteBallot, UUID> {
    List<CollectiveVoteBallot> findByVoteId(UUID voteId);
    Optional<CollectiveVoteBallot> findByVoteIdAndVoterId(UUID voteId, UUID voterId);
    boolean existsByVoteIdAndVoterId(UUID voteId, UUID voterId);
    long countByVoteId(UUID voteId);
}
