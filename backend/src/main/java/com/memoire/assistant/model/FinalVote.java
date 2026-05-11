package com.memoire.assistant.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "final_votes")
public class FinalVote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "voter_id", nullable = false)
    private UUID voterId;

    @Column(name = "voter_role", nullable = false)
    private String voterRole; // MANAGER | ADMIN

    @Column(name = "choice", nullable = false)
    private String choice; // APPROVED | REJECTED

    @Column(name = "rationale", columnDefinition = "TEXT")
    private String rationale;

    @Column(name = "voted_at", nullable = false)
    private LocalDateTime votedAt;

    @PrePersist
    public void prePersist() {
        if (votedAt == null) votedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getApplicationId() { return applicationId; }
    public void setApplicationId(UUID applicationId) { this.applicationId = applicationId; }

    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }

    public UUID getVoterId() { return voterId; }
    public void setVoterId(UUID voterId) { this.voterId = voterId; }

    public String getVoterRole() { return voterRole; }
    public void setVoterRole(String voterRole) { this.voterRole = voterRole; }

    public String getChoice() { return choice; }
    public void setChoice(String choice) { this.choice = choice; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public LocalDateTime getVotedAt() { return votedAt; }
    public void setVotedAt(LocalDateTime votedAt) { this.votedAt = votedAt; }
}
