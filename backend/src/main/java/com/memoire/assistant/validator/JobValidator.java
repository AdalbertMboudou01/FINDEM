package com.memoire.assistant.validator;

import com.memoire.assistant.dto.JobCreateRequest;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class JobValidator {

    private static final Set<String> VALID_LOCATIONS = new HashSet<>(Arrays.asList(
        "Paris", "Lyon", "Marseille", "Bordeaux", "Lille", "Toulouse", "Nice", "Nantes", "Strasbourg"
    ));

    private static final Set<String> VALID_DURATIONS = new HashSet<>(Arrays.asList(
        "6 mois", "12 mois", "24 mois", "36 mois"
    ));

    private static final Set<String> VALID_TECHNOLOGIES = new HashSet<>(Arrays.asList(
        "Java", "Python", "JavaScript", "TypeScript", "React", "Vue.js", "Angular", "Node.js", "Spring", "Django", "Flask",
        "Docker", "Kubernetes", "AWS", "Azure", "MongoDB", "PostgreSQL", "MySQL", "Redis", "Git", "Linux", "Windows"
    ));

    public void validate(JobCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La requête de création d'offre ne peut pas être nulle");
        }

        validateTitle(request.getTitle());
        validateDescription(request.getDescription());
        validateLocation(request.getLocation());
        validateContractDuration(request.getAlternanceRhythm());
        // validateTechnologies(request.getTechnologies()); // Temporairement désactivé
        // validateBlockingCriteria(request.getBlockingCriteria()); // Temporairement désactivé
    }

    private void validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Le titre de l'offre est obligatoire");
        }
        if (title.length() < 5 || title.length() > 100) {
            throw new IllegalArgumentException("Le titre doit contenir entre 5 et 100 caractères");
        }
    }

    private void validateDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("La description de l'offre est obligatoire");
        }
        if (description.length() < 20 || description.length() > 2000) {
            throw new IllegalArgumentException("La description doit contenir entre 20 et 2000 caractères");
        }
    }

    private void validateLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            throw new IllegalArgumentException("La localisation est obligatoire");
        }
        if (!VALID_LOCATIONS.contains(location)) {
            throw new IllegalArgumentException("La localisation doit être une ville française valide");
        }
    }

    private void validateContractDuration(String duration) {
        if (duration == null || duration.trim().isEmpty()) {
            throw new IllegalArgumentException("La duree du contrat est obligatoire");
        }
        if (!VALID_DURATIONS.contains(duration.trim().toLowerCase())) {
            throw new IllegalArgumentException("La duree du contrat doit etre: 6 mois, 12 mois, 24 mois ou 36 mois");
        }
    }

    private void validateTechnologies(List<String> technologies) {
        if (technologies == null || technologies.isEmpty()) {
            throw new IllegalArgumentException("Au moins une technologie doit être spécifiée");
        }
        if (technologies.size() > 10) {
            throw new IllegalArgumentException("Le nombre de technologies ne peut pas dépasser 10");
        }
        
        for (String tech : technologies) {
            if (tech == null || tech.trim().isEmpty()) {
                throw new IllegalArgumentException("Les technologies ne peuvent pas être vides");
            }
            if (!VALID_TECHNOLOGIES.contains(tech)) {
                throw new IllegalArgumentException("La technologie '" + tech + "' n'est pas valide");
            }
        }
    }

    private void validateBlockingCriteria(java.util.Map<String, Object> criteria) {
        if (criteria == null || criteria.isEmpty()) {
            throw new IllegalArgumentException("Les critères bloquants ne peuvent pas être vides");
        }
        
        // Valider les critères bloquants communs
        if (criteria.containsKey("minExperience") && criteria.get("minExperience") instanceof Number) {
            Number minExp = (Number) criteria.get("minExperience");
            if (minExp.intValue() < 0 || minExp.intValue() > 20) {
                throw new IllegalArgumentException("L'expérience minimale doit être entre 0 et 20 ans");
            }
        }
        
        if (criteria.containsKey("maxDistance") && criteria.get("maxDistance") instanceof Number) {
            Number maxDist = (Number) criteria.get("maxDistance");
            if (maxDist.intValue() < 0 || maxDist.intValue() > 500) {
                throw new IllegalArgumentException("La distance maximale doit être entre 0 et 500 km");
            }
        }
    }
}
