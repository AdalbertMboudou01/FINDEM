package com.memoire.assistant.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class DecisionDTO {

    private UUID id;
    private UUID applicationId;
    private String finalStatus;
    private String rationale;
    private UUID decidedBy;
    private LocalDateTime decidedAt;

    private String aiReview;

    // Résumé des avis pour affichage
    private List<DecisionInputDTO> inputs;
    private String blockingReason;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getApplicationId() { return applicationId; }
    public void setApplicationId(UUID applicationId) { this.applicationId = applicationId; }

    public String getFinalStatus() { return finalStatus; }
    public void setFinalStatus(String finalStatus) { this.finalStatus = finalStatus; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public UUID getDecidedBy() { return decidedBy; }
    public void setDecidedBy(UUID decidedBy) { this.decidedBy = decidedBy; }

    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }

    public String getAiReview() { return aiReview; }
    public void setAiReview(String aiReview) { this.aiReview = aiReview; }

    public List<DecisionInputDTO> getInputs() { return inputs; }
    public void setInputs(List<DecisionInputDTO> inputs) { this.inputs = inputs; }

    public String getBlockingReason() { return blockingReason; }
    public void setBlockingReason(String blockingReason) { this.blockingReason = blockingReason; }
}
