package com.memoire.assistant.service;

import org.springframework.stereotype.Service;

@Service
public class TestNotificationService extends NotificationService {
    
    @Override
    protected void sendEmail(String to, String subject, String content) {
        // Ne rien faire dans les tests - pas d'envoi d'email réel
        System.out.println("TEST: Email envoyé à " + to + " avec sujet: " + subject);
    }
}
