package com.memoire.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JobCreateRequest {
    @NotBlank(message = "Le titre est obligatoire")
    private String title;
    private String description;
    @NotBlank(message = "La localisation est obligatoire")
    private String location;
    private String alternanceRhythm;
    @NotBlank(message = "Le contexte de l'offre est obligatoire")
    private String contextePoste;
    @NotBlank(message = "Les missions détaillées sont obligatoires")
    private String missionsDetaillees;
    @NotEmpty(message = "Au moins une technologie requise est obligatoire")
    private List<String> technologies;
    private String serviceEntreprise;
    private Integer maxCandidatures;
    private boolean autoClose = true;
    private Map<String, Object> blockingCriteria;
    private String slug;
    private UUID companyId;
    private UUID ownerRecruiterId;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getAlternanceRhythm() { return alternanceRhythm; }
    public void setAlternanceRhythm(String alternanceRhythm) { this.alternanceRhythm = alternanceRhythm; }
    public String getContextePoste() { return contextePoste; }
    public void setContextePoste(String contextePoste) { this.contextePoste = contextePoste; }
    public String getMissionsDetaillees() { return missionsDetaillees; }
    public void setMissionsDetaillees(String missionsDetaillees) { this.missionsDetaillees = missionsDetaillees; }
    public List<String> getTechnologies() { return technologies; }
    public void setTechnologies(List<String> technologies) { this.technologies = technologies; }
    public String getServiceEntreprise() { return serviceEntreprise; }
    public void setServiceEntreprise(String serviceEntreprise) { this.serviceEntreprise = serviceEntreprise; }
    public Integer getMaxCandidatures() { return maxCandidatures; }
    public void setMaxCandidatures(Integer maxCandidatures) { this.maxCandidatures = maxCandidatures; }
    public boolean isAutoClose() { return autoClose; }
    public void setAutoClose(boolean autoClose) { this.autoClose = autoClose; }
    public Map<String, Object> getBlockingCriteria() { return blockingCriteria; }
    public void setBlockingCriteria(Map<String, Object> blockingCriteria) { this.blockingCriteria = blockingCriteria; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public UUID getOwnerRecruiterId() { return ownerRecruiterId; }
    public void setOwnerRecruiterId(UUID ownerRecruiterId) { this.ownerRecruiterId = ownerRecruiterId; }
}
