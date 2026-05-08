package com.memoire.assistant.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WebSocketPublisher {

    @Autowired
    private SimpMessagingTemplate messaging;

    /** Push une notification en temps réel à un utilisateur précis (par son email = principal). */
    public void pushNotification(String email, Object payload) {
        messaging.convertAndSendToUser(email, "/queue/notifications", payload);
    }

    /** Broadcast une mise à jour d'application à tous les membres d'une entreprise. */
    public void broadcastApplicationUpdate(UUID companyId, Object payload) {
        messaging.convertAndSend("/topic/company/" + companyId + "/applications", payload);
    }
}
