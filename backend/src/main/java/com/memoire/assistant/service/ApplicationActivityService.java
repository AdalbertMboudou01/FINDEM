package com.memoire.assistant.service;

import com.memoire.assistant.model.Application;
import com.memoire.assistant.model.ApplicationActivity;
import com.memoire.assistant.model.ApplicationActivity.ActorType;
import com.memoire.assistant.model.ApplicationActivity.EventType;
import com.memoire.assistant.repository.ApplicationActivityRepository;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ApplicationActivityService {

    @Autowired
    private ApplicationActivityRepository activityRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    /**
     * Enregistre un événement dans la timeline d'une candidature.
     * Point d'entrée principal pour tous les autres services.
     */
    public ApplicationActivity logEvent(UUID applicationId, EventType eventType, Map<String, Object> payload) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable: " + applicationId));

        UUID companyId = TenantContext.getCompanyId();
        UUID actorId = TenantContext.getRecruiterId() != null
                ? TenantContext.getRecruiterId()
                : TenantContext.getUserId();
        ActorType actorType = TenantContext.getRecruiterId() != null ? ActorType.RECRUITER : ActorType.USER;

        return save(application, companyId, actorId, actorType, eventType, payload, "ALL");
    }

    /**
     * Enregistre un événement système (sans acteur humain).
     */
    public ApplicationActivity logSystemEvent(UUID applicationId, UUID companyId, EventType eventType, Map<String, Object> payload) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable: " + applicationId));

        return save(application, companyId, null, ActorType.SYSTEM, eventType, payload, "ALL");
    }

    /**
     * Enregistre un événement interne (visible uniquement par l'équipe).
     */
    public ApplicationActivity logInternalEvent(UUID applicationId, EventType eventType, Map<String, Object> payload) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Candidature introuvable: " + applicationId));

        UUID companyId = TenantContext.getCompanyId();
        UUID actorId = TenantContext.getRecruiterId() != null
                ? TenantContext.getRecruiterId()
                : TenantContext.getUserId();
        ActorType actorType = TenantContext.getRecruiterId() != null ? ActorType.RECRUITER : ActorType.USER;

        return save(application, companyId, actorId, actorType, eventType, payload, "INTERNAL");
    }

    public List<ApplicationActivity> getActivityForApplication(UUID applicationId) {
        UUID companyId = TenantContext.getCompanyId();
        return activityRepository.findByApplication_ApplicationIdAndCompanyIdOrderByCreatedAtAsc(applicationId, companyId);
    }

    public List<ApplicationActivity> getCompanyFeed() {
        UUID companyId = TenantContext.getCompanyId();
        return activityRepository.findByCompanyIdOrderByCreatedAtDesc(companyId);
    }

    private ApplicationActivity save(Application application, UUID companyId, UUID actorId,
                                     ActorType actorType, EventType eventType,
                                     Map<String, Object> payload, String visibility) {
        ApplicationActivity activity = new ApplicationActivity();
        activity.setApplication(application);
        activity.setCompanyId(companyId);
        activity.setActorId(actorId);
        activity.setActorType(actorType);
        activity.setEventType(eventType);
        activity.setPayload(payload != null ? payload : Map.of());
        activity.setVisibility(visibility);
        return activityRepository.save(activity);
    }
}
