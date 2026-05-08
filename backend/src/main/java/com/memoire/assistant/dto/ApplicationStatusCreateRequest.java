package com.memoire.assistant.dto;

import jakarta.validation.constraints.NotBlank;

public class ApplicationStatusCreateRequest {
    @NotBlank(message = "Le code est obligatoire")
    private String code;
    @NotBlank(message = "Le libellé est obligatoire")
    private String label;
    // Getters & Setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
