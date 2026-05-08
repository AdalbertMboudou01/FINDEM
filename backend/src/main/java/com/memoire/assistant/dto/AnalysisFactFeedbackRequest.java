package com.memoire.assistant.dto;

public class AnalysisFactFeedbackRequest {

    private String dimension;
    private String finding;
    private String evidence;
    private String decision;
    private String correctedFinding;
    private String reviewerComment;

    public String getDimension() {
        return dimension;
    }

    public void setDimension(String dimension) {
        this.dimension = dimension;
    }

    public String getFinding() {
        return finding;
    }

    public void setFinding(String finding) {
        this.finding = finding;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getCorrectedFinding() {
        return correctedFinding;
    }

    public void setCorrectedFinding(String correctedFinding) {
        this.correctedFinding = correctedFinding;
    }

    public String getReviewerComment() {
        return reviewerComment;
    }

    public void setReviewerComment(String reviewerComment) {
        this.reviewerComment = reviewerComment;
    }
}
