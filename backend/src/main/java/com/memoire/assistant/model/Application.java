package com.memoire.assistant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "applications")
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID applicationId;

    @ManyToOne
    @JoinColumn(name = "candidate_id")
    private Candidate candidate;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne
    @JoinColumn(name = "status_id")
    private ApplicationStatus status;

    private Date createdAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "semantic_cache", columnDefinition = "JSONB")
    private String semanticCache;

    @Column(name = "semantic_cache_at")
    private Instant semanticCacheAt;

    // Getters & Setters
    public UUID getApplicationId() {
        return applicationId;
    }
    
    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }
    
    public Candidate getCandidate() {
        return candidate;
    }
    
    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }
    
    public Job getJob() {
        return job;
    }
    
    public void setJob(Job job) {
        this.job = job;
    }
    
    public ApplicationStatus getStatus() {
        return status;
    }
    
    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getSemanticCache() {
        return semanticCache;
    }

    public void setSemanticCache(String semanticCache) {
        this.semanticCache = semanticCache;
    }

    public Instant getSemanticCacheAt() {
        return semanticCacheAt;
    }

    public void setSemanticCacheAt(Instant semanticCacheAt) {
        this.semanticCacheAt = semanticCacheAt;
    }
}
