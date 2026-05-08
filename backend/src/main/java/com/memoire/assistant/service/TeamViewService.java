package com.memoire.assistant.service;

import com.memoire.assistant.model.Application;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.DecisionInputRepository;
import com.memoire.assistant.repository.DecisionRepository;
import com.memoire.assistant.repository.TaskRepository;
import com.memoire.assistant.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Fournit les vues d'équipe filtrées sur les candidatures.
 *
 * Views:
 *   attente_avis     — candidatures sans aucun avis décision
 *   pret_a_decider   — candidatures avec ≥1 avis mais sans décision finale
 *   attente_candidat — candidatures avec une tâche ouverte (TODO/IN_PROGRESS) en retard
 *   activite_offre   — toutes les candidatures d'une offre (requiert offerId)
 */
@Service
public class TeamViewService {

    @Autowired private ApplicationRepository applicationRepository;
    @Autowired private DecisionInputRepository decisionInputRepository;
    @Autowired private DecisionRepository decisionRepository;
    @Autowired private TaskRepository taskRepository;

    public List<Application> getView(String view, UUID offerId) {
        UUID companyId = TenantContext.getCompanyId();
        List<Application> all = applicationRepository.findByJob_Company_CompanyId(companyId);

        return switch (view) {
            case "attente_avis" -> filterAttenteAvis(all, companyId);
            case "pret_a_decider" -> filterPretADecider(all, companyId);
            case "attente_candidat" -> filterAttenteCandidatOverdue(all, companyId);
            case "activite_offre" -> filterActiviteOffre(all, offerId);
            default -> all;
        };
    }

    /** Candidatures sans aucun avis enregistré */
    private List<Application> filterAttenteAvis(List<Application> all, UUID companyId) {
        Set<UUID> withInputs = decisionInputRepository
                .findAll().stream()
                .filter(d -> d.getCompanyId().equals(companyId))
                .map(d -> d.getApplicationId())
                .collect(Collectors.toSet());
        return all.stream()
                .filter(a -> !withInputs.contains(a.getApplicationId()))
                .collect(Collectors.toList());
    }

    /** Candidatures avec ≥1 avis mais sans décision finale */
    private List<Application> filterPretADecider(List<Application> all, UUID companyId) {
        Set<UUID> withInputs = decisionInputRepository
                .findAll().stream()
                .filter(d -> d.getCompanyId().equals(companyId))
                .map(d -> d.getApplicationId())
                .collect(Collectors.toSet());
        Set<UUID> withDecision = decisionRepository
                .findAll().stream()
                .filter(d -> d.getCompanyId().equals(companyId))
                .map(d -> d.getApplicationId())
                .collect(Collectors.toSet());
        return all.stream()
                .filter(a -> withInputs.contains(a.getApplicationId())
                          && !withDecision.contains(a.getApplicationId()))
                .collect(Collectors.toList());
    }

    /** Candidatures avec une tâche ouverte en retard */
    private List<Application> filterAttenteCandidatOverdue(List<Application> all, UUID companyId) {
        Set<UUID> overdueAppIds = taskRepository
                .findByCompanyIdAndStatusNotAndDueDateBeforeOrderByDueDateAsc(
                        companyId, com.memoire.assistant.model.Task.Status.DONE, LocalDate.now())
                .stream()
                .map(t -> t.getApplicationId())
                .collect(Collectors.toSet());
        return all.stream()
                .filter(a -> overdueAppIds.contains(a.getApplicationId()))
                .collect(Collectors.toList());
    }

    /** Toutes les candidatures d'une offre donnée */
    private List<Application> filterActiviteOffre(List<Application> all, UUID offerId) {
        if (offerId == null) return all;
        return all.stream()
                .filter(a -> a.getJob() != null && offerId.equals(a.getJob().getJobId()))
                .collect(Collectors.toList());
    }
}
