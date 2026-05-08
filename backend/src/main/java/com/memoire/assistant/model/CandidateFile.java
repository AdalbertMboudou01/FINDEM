package com.memoire.assistant.model;

import jakarta.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "candidate_files")
public class CandidateFile {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID fileId;

    @ManyToOne
    @JoinColumn(name = "candidate_id")
    private Candidate candidate;

    private String fileName;
    private String originalFileName;
    private String fileType; // CV, PORTFOLIO, COVER_LETTER
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private Date uploadedAt;

    // Constructors
    public CandidateFile() {}

    public CandidateFile(Candidate candidate, String fileName, String originalFileName, String fileType, String filePath, Long fileSize, String mimeType) {
        this.candidate = candidate;
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.fileType = fileType;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.uploadedAt = new Date();
    }

    // Getters & Setters
    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Date getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Date uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
