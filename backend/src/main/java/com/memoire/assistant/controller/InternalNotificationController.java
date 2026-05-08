package com.memoire.assistant.controller;

import com.memoire.assistant.service.InternalNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/internal-notifications")
public class InternalNotificationController {

    @Autowired
    private InternalNotificationService notificationService;

    /** Récupère les notifications de l'utilisateur courant */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMyNotifications() {
        List<Map<String, Object>> notifications = notificationService.getMyNotifications();
        long unreadCount = notificationService.getUnreadCount();
        return ResponseEntity.ok(Map.of(
                "notifications", notifications,
                "unreadCount", unreadCount
        ));
    }

    /** Marque une notification comme lue */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    /** Marque toutes les notifications comme lues */
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok().build();
    }
}
