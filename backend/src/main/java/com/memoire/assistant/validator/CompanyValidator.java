package com.memoire.assistant.validator;

import com.memoire.assistant.dto.CompanyCreateRequest;
import org.springframework.stereotype.Component;

@Component
public class CompanyValidator {

    public void validate(CompanyCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requête de création d'entreprise ne peut pas être nulle");
        }

        validateName(request.getName());
        validateSector(request.getSector());
        validateSize(request.getSize());
        validateWebsite(request.getWebsite());
        validatePlan(request.getPlan());
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de l'entreprise est obligatoire");
        }
        if (name.length() < 2 || name.length() > 100) {
            throw new IllegalArgumentException("Le nom de l'entreprise doit contenir entre 2 et 100 caractères");
        }
    }

    private void validateSector(String sector) {
        if (sector == null || sector.trim().isEmpty()) {
            throw new IllegalArgumentException("Le secteur d'activité est obligatoire");
        }
        if (sector.length() < 2 || sector.length() > 50) {
            throw new IllegalArgumentException("Le secteur doit contenir entre 2 et 50 caractères");
        }
    }

    private void validateSize(String size) {
        if (size == null || size.trim().isEmpty()) {
            throw new IllegalArgumentException("La taille de l'entreprise est obligatoire");
        }
        String[] validSizes = {"Startup", "PME", "ETI", "Grande Entreprise"};
        boolean isValid = false;
        for (String validSize : validSizes) {
            if (validSize.equalsIgnoreCase(size)) {
                isValid = true;
                break;
            }
        }
        if (!isValid) {
            throw new IllegalArgumentException("La taille doit être: Startup, PME, ETI ou Grande Entreprise");
        }
    }

    private void validateWebsite(String website) {
        if (website != null && !website.trim().isEmpty()) {
            if (!website.startsWith("http://") && !website.startsWith("https://")) {
                throw new IllegalArgumentException("L'URL du site web n'est pas valide");
            }
        }
    }

    private void validatePlan(String plan) {
        if (plan == null || plan.trim().isEmpty()) {
            throw new IllegalArgumentException("Le plan d'abonnement est obligatoire");
        }
        String[] validPlans = {"Basic", "Pro", "Enterprise"};
        boolean isValid = false;
        for (String validPlan : validPlans) {
            if (validPlan.equalsIgnoreCase(plan)) {
                isValid = true;
                break;
            }
        }
        if (!isValid) {
            throw new IllegalArgumentException("Le plan doit être: Basic, Pro ou Enterprise");
        }
    }
}
