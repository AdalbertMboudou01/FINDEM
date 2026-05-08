package com.memoire.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class CompanyCreateRequest {
    @NotBlank(message = "Le nom est obligatoire")
    private String name;
    @NotBlank(message = "Le secteur est obligatoire")
    private String sector;
    @NotBlank(message = "La taille est obligatoire")
    private String size;
    private String website;
    private String plan;
    private Map<String, Object> config;
    // Getters & Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }
}
