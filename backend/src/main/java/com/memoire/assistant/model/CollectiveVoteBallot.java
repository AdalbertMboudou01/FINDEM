package com.memoire.assistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "collective_vote_ballots")
public class CollectiveVoteBallot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vote_id", nullable = false)
    private UUID voteId;

    @Column(name = "voter_id", nullable = false)
    private UUID voterId;

    @Column(name = "choice", nullable = false)
    private String choice; // APPROVE | REJECT | ABSTAIN

    @Column(name = "voted_at", nullable = false)
    private LocalDateTime votedAt;

    @PrePersist
    public void prePersist() {
        if (votedAt == null) votedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getVoteId() { return voteId; }
    public void setVoteId(UUID voteId) { this.voteId = voteId; }

    public UUID getVoterId() { return voterId; }
    public void setVoterId(UUID voterId) { this.voterId = voterId; }

    public String getChoice() { return choice; }
    public void setChoice(String choice) { this.choice = choice; }

    public LocalDateTime getVotedAt() { return votedAt; }
    public void setVotedAt(LocalDateTime votedAt) { this.votedAt = votedAt; }
}
