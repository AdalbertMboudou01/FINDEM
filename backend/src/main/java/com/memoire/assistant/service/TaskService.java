package com.memoire.assistant.service;

import com.memoire.assistant.dto.TaskCreateRequest;
import com.memoire.assistant.dto.TaskDTO;
import com.memoire.assistant.model.AIPersona;
import com.memoire.assistant.model.ApplicationActivity.EventType;
import com.memoire.assistant.model.InternalNotification;
import com.memoire.assistant.model.Task;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.TaskRepository;
import com.memoire.assistant.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ApplicationActivityService activityService;

    @Autowired
    private InternalNotificationService notificationService;

    @Autowired
    private AITaskExecutionService aiTaskExecutionService;

    public List<TaskDTO> getTasksForApplication(UUID applicationId) {
        UUID companyId = TenantContext.getCompanyId();
        return taskRepository
                .findByApplicationIdAndCompanyIdOrderByCreatedAtDesc(applicationId, companyId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public TaskDTO createTask(UUID applicationId, TaskCreateRequest req) {
        UUID companyId = TenantContext.getCompanyId();

        applicationRepository.findById(applicationId).orElseThrow(
                () -> new RuntimeException("Application non trouvée"));

        UUID actorId = TenantContext.getRecruiterId() != null
                ? TenantContext.getRecruiterId()
                : TenantContext.getUserId();

        Task task = new Task();
        task.setApplicationId(applicationId);
        task.setCompanyId(companyId);
        task.setTitle(req.getTitle());
        task.setDescription(req.getDescription());
        task.setAssigneeId(req.getAssigneeId());
        task.setDueDate(req.getDueDate());
        task.setTaskType(req.getTaskType());
        task.setStatus(Task.Status.TODO);
        if (req.getPriority() != null) {
            task.setPriority(Task.Priority.valueOf(req.getPriority()));
        }
        task.setCreatedBy(actorId);

        Task saved = taskRepository.save(task);

        activityService.logEvent(applicationId, EventType.TASK_CREATED, Map.of(
                "taskId", saved.getId().toString(),
                "title", saved.getTitle(),
                "assigneeId", saved.getAssigneeId() != null ? saved.getAssigneeId().toString() : ""
        ));

        if (AIPersona.FINDEM_WORKER_ID.equals(saved.getAssigneeId())) {
            aiTaskExecutionService.execute(saved.getId(), applicationId, companyId, saved.getTaskType());
        } else if (saved.getAssigneeId() != null) {
            notificationService.notify(
                    saved.getAssigneeId(), companyId,
                    InternalNotification.Type.TASK_ASSIGNED,
                    "Une tâche vous a été assignée",
                    saved.getTitle(),
                    "candidate", applicationId
            );
        }

        return toDTO(saved);
    }

    public TaskDTO updateStatus(UUID taskId, String newStatus) {
        UUID companyId = TenantContext.getCompanyId();
        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getCompanyId().equals(companyId))
                .orElseThrow(() -> new RuntimeException("Tâche non trouvée"));

        Task.Status previousStatus = task.getStatus();
        task.setStatus(Task.Status.valueOf(newStatus));
        Task saved = taskRepository.save(task);

        if (previousStatus != Task.Status.DONE && saved.getStatus() == Task.Status.DONE) {
            activityService.logEvent(saved.getApplicationId(), EventType.TASK_DONE, Map.of(
                    "taskId", saved.getId().toString(),
                    "title", saved.getTitle()
            ));
        }

        return toDTO(saved);
    }

    public void deleteTask(UUID taskId) {
        UUID companyId = TenantContext.getCompanyId();
        Task task = taskRepository.findById(taskId)
                .filter(t -> t.getCompanyId().equals(companyId))
                .orElseThrow(() -> new RuntimeException("Tâche non trouvée"));
        taskRepository.delete(task);
    }

    public List<TaskDTO> getMyTasks() {
        UUID companyId = TenantContext.getCompanyId();
        UUID assigneeId = TenantContext.getRecruiterId() != null
                ? TenantContext.getRecruiterId()
                : TenantContext.getUserId();
        return taskRepository
                .findByAssigneeIdAndCompanyIdOrderByDueDateAsc(assigneeId, companyId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<TaskDTO> getOverdueTasks() {
        UUID companyId = TenantContext.getCompanyId();
        return taskRepository
                .findByCompanyIdAndStatusNotAndDueDateBeforeOrderByDueDateAsc(
                        companyId, Task.Status.DONE, LocalDate.now())
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private TaskDTO toDTO(Task t) {
        TaskDTO dto = new TaskDTO();
        dto.setId(t.getId());
        dto.setApplicationId(t.getApplicationId());
        dto.setCompanyId(t.getCompanyId());
        dto.setTitle(t.getTitle());
        dto.setDescription(t.getDescription());
        dto.setAssigneeId(t.getAssigneeId());
        dto.setDueDate(t.getDueDate());
        dto.setStatus(t.getStatus().name());
        dto.setPriority(t.getPriority().name());
        dto.setCreatedBy(t.getCreatedBy());
        dto.setCreatedAt(t.getCreatedAt());
        dto.setUpdatedAt(t.getUpdatedAt());
        dto.setOverdue(t.getDueDate() != null
                && t.getStatus() != Task.Status.DONE
                && t.getDueDate().isBefore(LocalDate.now()));
        dto.setTaskType(t.getTaskType());
        dto.setAiResult(t.getAiResult());
        return dto;
    }
}
