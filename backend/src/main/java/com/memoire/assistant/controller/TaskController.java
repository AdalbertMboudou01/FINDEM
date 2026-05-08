package com.memoire.assistant.controller;

import com.memoire.assistant.dto.TaskCreateRequest;
import com.memoire.assistant.dto.TaskDTO;
import com.memoire.assistant.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class TaskController {

    @Autowired
    private TaskService taskService;

    /** Lister les tâches d'une candidature */
    @GetMapping("/applications/{applicationId}/tasks")
    public ResponseEntity<List<TaskDTO>> getTasksForApplication(
            @PathVariable UUID applicationId) {
        return ResponseEntity.ok(taskService.getTasksForApplication(applicationId));
    }

    /** Créer une tâche sur une candidature */
    @PostMapping("/applications/{applicationId}/tasks")
    public ResponseEntity<TaskDTO> createTask(
            @PathVariable UUID applicationId,
            @RequestBody TaskCreateRequest req) {
        return ResponseEntity.ok(taskService.createTask(applicationId, req));
    }

    /** Mettre à jour le statut d'une tâche */
    @PatchMapping("/tasks/{taskId}/status")
    public ResponseEntity<TaskDTO> updateStatus(
            @PathVariable UUID taskId,
            @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        return ResponseEntity.ok(taskService.updateStatus(taskId, newStatus));
    }

    /** Supprimer une tâche */
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID taskId) {
        taskService.deleteTask(taskId);
        return ResponseEntity.noContent().build();
    }

    /** Mes tâches (assignées à l'utilisateur courant) */
    @GetMapping("/tasks/mine")
    public ResponseEntity<List<TaskDTO>> getMyTasks() {
        return ResponseEntity.ok(taskService.getMyTasks());
    }

    /** Tâches en retard */
    @GetMapping("/tasks/overdue")
    public ResponseEntity<List<TaskDTO>> getOverdueTasks() {
        return ResponseEntity.ok(taskService.getOverdueTasks());
    }
}
