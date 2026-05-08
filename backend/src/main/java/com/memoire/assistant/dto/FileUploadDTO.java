package com.memoire.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class FileUploadDTO {
    @NotNull(message = "L'ID du candidat est obligatoire")
    private UUID candidateId;
    
    @NotBlank(message = "Le type de fichier est obligatoire")
    private String fileType; // CV, PORTFOLIO, COVER_LETTER
    
    private String githubUrl;
    private String portfolioUrl;

    // Constructors
    public FileUploadDTO() {}

    public FileUploadDTO(UUID candidateId, String fileType) {
        this.candidateId = candidateId;
        this.fileType = fileType;
    }

    // Getters & Setters
    public UUID getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(UUID candidateId) {
        this.candidateId = candidateId;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getPortfolioUrl() {
        return portfolioUrl;
    }

    public void setPortfolioUrl(String portfolioUrl) {
        this.portfolioUrl = portfolioUrl;
    }
}
