package com.memoire.assistant.service;

import com.memoire.assistant.model.ApplicationStatus;
import com.memoire.assistant.repository.ApplicationStatusRepository;
import com.memoire.assistant.repository.DecisionInputRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApplicationStatusService {
    @Autowired
    private ApplicationStatusRepository applicationStatusRepository;

    @Autowired
    private DecisionInputRepository decisionInputRepository;

    private static final Map<String, List<String>> ALLOWED_TRANSITIONS = new LinkedHashMap<>();

    static {
        ALLOWED_TRANSITIONS.put("nouveau",               List.of("en_etude", "non_retenu", "vivier"));
        ALLOWED_TRANSITIONS.put("en_etude",              List.of("en_attente_avis", "non_retenu", "vivier"));
        ALLOWED_TRANSITIONS.put("en_attente_avis",       List.of("entretien", "non_retenu", "vivier"));
        ALLOWED_TRANSITIONS.put("entretien",             List.of("retenu", "non_retenu", "vivier"));
        ALLOWED_TRANSITIONS.put("retenu",                List.of("embauche", "non_retenu", "vivier"));
        ALLOWED_TRANSITIONS.put("embauche",              List.of());
        ALLOWED_TRANSITIONS.put("non_retenu",            List.of("vivier", "en_etude"));
        ALLOWED_TRANSITIONS.put("vivier",                List.of("en_etude"));
        // Legacy statuses
        ALLOWED_TRANSITIONS.put("en_attente",            List.of("en_etude", "non_retenu", "vivier"));
        ALLOWED_TRANSITIONS.put("a_revoir_manuellement", List.of("entretien", "non_retenu", "vivier"));
        ALLOWED_TRANSITIONS.put("retenu_entretien",      List.of("retenu", "non_retenu", "vivier"));
    }

    public List<ApplicationStatus> getAllStatuses() {
        return applicationStatusRepository.findAll();
    }

    public Optional<ApplicationStatus> getStatusById(UUID id) {
        return applicationStatusRepository.findById(id);
    }

    public ApplicationStatus saveStatus(ApplicationStatus status) {
        return applicationStatusRepository.save(status);
    }

    public void deleteStatus(UUID id) {
        applicationStatusRepository.deleteById(id);
    }

    public List<String> getAllowedTransitions(String currentCode) {
        return ALLOWED_TRANSITIONS.getOrDefault(
            currentCode == null ? "" : currentCode.toLowerCase(), List.of());
    }

    public void validateTransition(String fromCode, String toCode, UUID applicationId) {
        List<String> allowed = getAllowedTransitions(fromCode == null ? "" : fromCode);
        if (!allowed.contains(toCode)) {
            throw new IllegalStateException(
                "Transition interdite : " + fromCode + " → " + toCode +
                ". Transitions autorisées depuis " + fromCode + " : " + allowed
            );
        }
        if ("entretien".equals(toCode)) {
            long count = decisionInputRepository.countByApplicationId(applicationId);
            if (count == 0) {
                throw new IllegalStateException(
                    "Impossible de planifier un entretien sans au moins un avis enregistré."
                );
            }
        }
    }
}
