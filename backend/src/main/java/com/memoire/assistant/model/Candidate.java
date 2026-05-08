package com.memoire.assistant.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "candidates")
public class Candidate {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID candidateId;

    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String location;

    private String school;
    private Boolean consent;
    private Boolean inPool = false; // Vivier
    private String githubUrl;
    private String portfolioUrl;
    
    // Champ pour le CV
    @Column(name = "cv_path")
    private String cvPath;
    
    @Column(name = "cv_file_name")
    private String cvFileName;
    
    @Column(name = "cv_content_type")
    private String cvContentType;
    
    private Date createdAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "github_cache", columnDefinition = "jsonb")
    private Map<String, Object> githubCache;

    @Column(name = "github_cache_at")
    private Date githubCacheAt;

    // Getters & Setters
    public UUID getCandidateId() {
        return candidateId;
    }
    
    public void setCandidateId(UUID candidateId) {
        this.candidateId = candidateId;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getSchool() {
        return school;
    }
    
    public void setSchool(String school) {
        this.school = school;
    }
    
    public Boolean getConsent() {
        return consent;
    }
    
    public void setConsent(Boolean consent) {
        this.consent = consent;
    }

    public Boolean getInPool() {
        return inPool;
    }

    public void setInPool(Boolean inPool) {
        this.inPool = inPool;
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
    
    // Getters et setters pour les champs CV
    public String getCvPath() {
        return cvPath;
    }
    
    public void setCvPath(String cvPath) {
        this.cvPath = cvPath;
    }
    
    public String getCvFileName() {
        return cvFileName;
    }
    
    public void setCvFileName(String cvFileName) {
        this.cvFileName = cvFileName;
    }
    
    public String getCvContentType() {
        return cvContentType;
    }
    
    public void setCvContentType(String cvContentType) {
        this.cvContentType = cvContentType;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Map<String, Object> getGithubCache() {
        return githubCache;
    }

    public void setGithubCache(Map<String, Object> githubCache) {
        this.githubCache = githubCache;
    }

    public Date getGithubCacheAt() {
        return githubCacheAt;
    }

    public void setGithubCacheAt(Date githubCacheAt) {
        this.githubCacheAt = githubCacheAt;
    }
}
