package com.memoire.assistant.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID jobId;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne
    @JoinColumn(name = "owner_recruiter_id")
    private Recruiter ownerRecruiter;

    @Column(columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String location;

    @Column(name = "duration_contract", columnDefinition = "TEXT")
    private String alternanceRhythm;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> blockingCriteria;

    @ElementCollection
    private List<String> technologies;

    @Column(columnDefinition = "TEXT")
    private String slug;
    
    // Champs pour le formulaire offre
    @Column(name = "contexte_poste", columnDefinition = "TEXT")
    private String contextePoste;
    
    @Column(name = "missions_detaillees", columnDefinition = "TEXT")
    private String missionsDetaillees;
    
    @Column(name = "service_entreprise", columnDefinition = "TEXT")
    private String serviceEntreprise;
    
    @Column(name = "statut", columnDefinition = "TEXT")
    private String statut; // ouvert/clôturé

    @Column(name = "max_candidatures")
    private Integer maxCandidatures;

    @Column(name = "auto_close", nullable = false)
    private boolean autoClose = true;

    private Date createdAt;

    // Getters & Setters
    public UUID getJobId() {
        return jobId;
    }
    
    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }
    
    public Company getCompany() {
        return company;
    }
    
    public void setCompany(Company company) {
        this.company = company;
    }
    
    public Recruiter getOwnerRecruiter() {
        return ownerRecruiter;
    }
    
    public void setOwnerRecruiter(Recruiter ownerRecruiter) {
        this.ownerRecruiter = ownerRecruiter;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public String getAlternanceRhythm() {
        return alternanceRhythm;
    }
    
    public void setAlternanceRhythm(String alternanceRhythm) {
        this.alternanceRhythm = alternanceRhythm;
    }
    
    public Map<String, Object> getBlockingCriteria() {
        return blockingCriteria;
    }
    
    public void setBlockingCriteria(Map<String, Object> blockingCriteria) {
        this.blockingCriteria = blockingCriteria;
    }
    
    public List<String> getTechnologies() {
        return technologies;
    }
    
    public void setTechnologies(List<String> technologies) {
        this.technologies = technologies;
    }
    
    public String getSlug() {
        return slug;
    }
    
    public void setSlug(String slug) {
        this.slug = slug;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    // Getters et setters pour les nouveaux champs
    public String getContextePoste() {
        return contextePoste;
    }
    
    public void setContextePoste(String contextePoste) {
        this.contextePoste = contextePoste;
    }
    
    public String getMissionsDetaillees() {
        return missionsDetaillees;
    }
    
    public void setMissionsDetaillees(String missionsDetaillees) {
        this.missionsDetaillees = missionsDetaillees;
    }
    
    public String getServiceEntreprise() {
        return serviceEntreprise;
    }
    
    public void setServiceEntreprise(String serviceEntreprise) {
        this.serviceEntreprise = serviceEntreprise;
    }
    
    public String getStatut() {
        return statut;
    }
    
    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Integer getMaxCandidatures() {
        return maxCandidatures;
    }

    public void setMaxCandidatures(Integer maxCandidatures) {
        this.maxCandidatures = maxCandidatures;
    }

    public boolean isAutoClose() {
        return autoClose;
    }

    public void setAutoClose(boolean autoClose) {
        this.autoClose = autoClose;
    }
}
