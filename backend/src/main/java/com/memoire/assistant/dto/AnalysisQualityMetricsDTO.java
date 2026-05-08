package com.memoire.assistant.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AnalysisQualityMetricsDTO {

    private int windowDays;
    private LocalDateTime generatedAt;
    private int feedbackEvents;
    private int reviewedFacts;
    private int reviewedApplications;
    private int confirmedFacts;
    private int correctedFacts;
    private int rejectedFacts;
    private double precisionScore;
    private double correctionRate;
    private double rejectionRate;
    private List<DimensionMetricsDTO> byDimension;

    public int getWindowDays() {
        return windowDays;
    }

    public void setWindowDays(int windowDays) {
        this.windowDays = windowDays;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public int getFeedbackEvents() {
        return feedbackEvents;
    }

    public void setFeedbackEvents(int feedbackEvents) {
        this.feedbackEvents = feedbackEvents;
    }

    public int getReviewedFacts() {
        return reviewedFacts;
    }

    public void setReviewedFacts(int reviewedFacts) {
        this.reviewedFacts = reviewedFacts;
    }

    public int getReviewedApplications() {
        return reviewedApplications;
    }

    public void setReviewedApplications(int reviewedApplications) {
        this.reviewedApplications = reviewedApplications;
    }

    public int getConfirmedFacts() {
        return confirmedFacts;
    }

    public void setConfirmedFacts(int confirmedFacts) {
        this.confirmedFacts = confirmedFacts;
    }

    public int getCorrectedFacts() {
        return correctedFacts;
    }

    public void setCorrectedFacts(int correctedFacts) {
        this.correctedFacts = correctedFacts;
    }

    public int getRejectedFacts() {
        return rejectedFacts;
    }

    public void setRejectedFacts(int rejectedFacts) {
        this.rejectedFacts = rejectedFacts;
    }

    public double getPrecisionScore() {
        return precisionScore;
    }

    public void setPrecisionScore(double precisionScore) {
        this.precisionScore = precisionScore;
    }

    public double getCorrectionRate() {
        return correctionRate;
    }

    public void setCorrectionRate(double correctionRate) {
        this.correctionRate = correctionRate;
    }

    public double getRejectionRate() {
        return rejectionRate;
    }

    public void setRejectionRate(double rejectionRate) {
        this.rejectionRate = rejectionRate;
    }

    public List<DimensionMetricsDTO> getByDimension() {
        return byDimension;
    }

    public void setByDimension(List<DimensionMetricsDTO> byDimension) {
        this.byDimension = byDimension;
    }

    public static class DimensionMetricsDTO {
        private String dimension;
        private int reviewedFacts;
        private int confirmedFacts;
        private int correctedFacts;
        private int rejectedFacts;
        private double precisionScore;

        public String getDimension() {
            return dimension;
        }

        public void setDimension(String dimension) {
            this.dimension = dimension;
        }

        public int getReviewedFacts() {
            return reviewedFacts;
        }

        public void setReviewedFacts(int reviewedFacts) {
            this.reviewedFacts = reviewedFacts;
        }

        public int getConfirmedFacts() {
            return confirmedFacts;
        }

        public void setConfirmedFacts(int confirmedFacts) {
            this.confirmedFacts = confirmedFacts;
        }

        public int getCorrectedFacts() {
            return correctedFacts;
        }

        public void setCorrectedFacts(int correctedFacts) {
            this.correctedFacts = correctedFacts;
        }

        public int getRejectedFacts() {
            return rejectedFacts;
        }

        public void setRejectedFacts(int rejectedFacts) {
            this.rejectedFacts = rejectedFacts;
        }

        public double getPrecisionScore() {
            return precisionScore;
        }

        public void setPrecisionScore(double precisionScore) {
            this.precisionScore = precisionScore;
        }
    }
}