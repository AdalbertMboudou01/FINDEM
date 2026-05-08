package com.memoire.assistant.dto;

public class AnalysisFactDTO {

    private String dimension;
    private String finding;
    private String evidence;
    private double confidence;
    private String sourceQuestion;

    public AnalysisFactDTO() {
    }

    public AnalysisFactDTO(String dimension, String finding, String evidence, double confidence, String sourceQuestion) {
        this.dimension = dimension;
        this.finding = finding;
        this.evidence = evidence;
        this.confidence = confidence;
        this.sourceQuestion = sourceQuestion;
    }

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

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getSourceQuestion() {
        return sourceQuestion;
    }

    public void setSourceQuestion(String sourceQuestion) {
        this.sourceQuestion = sourceQuestion;
    }
}
