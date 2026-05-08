package com.memoire.assistant.service;

import com.memoire.assistant.dto.ChatAnswerAnalysisDTO;
import com.memoire.assistant.model.AIPersona;
import com.memoire.assistant.model.Application;
import com.memoire.assistant.model.ApplicationActivity.EventType;
import com.memoire.assistant.model.Candidate;
import com.memoire.assistant.model.Job;
import com.memoire.assistant.model.Task;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AITaskExecutionServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private ChatAnswerService chatAnswerService;
    @Mock private ApplicationActivityService activityService;
    @Mock private AIClientService aiClientService;
    @Mock private AIPersonaCommentService aiPersonaCommentService;

    @InjectMocks private AITaskExecutionService service;

    @Test
    void execute_generatesResultCompletesTaskAndPostsComment() {
        UUID taskId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Task task = task(taskId);
        Application application = application(applicationId);

        when(aiClientService.isEnabled()).thenReturn(true);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(chatAnswerService.analyzeChatAnswers(applicationId)).thenReturn(analysis(applicationId));
        when(aiClientService.complete(anyString(), anyString(), eq(0.4))).thenReturn("Resultat pret");

        service.execute(taskId, applicationId, companyId, "PROFILE_SUMMARY");

        assertEquals("Resultat pret", task.getAiResult());
        assertEquals(Task.Status.DONE, task.getStatus());
        verify(taskRepository).save(task);
        verify(aiPersonaCommentService).postInternalComment(
                eq(applicationId),
                eq(companyId),
                eq(AIPersona.FINDEM_WORKER_ID),
                contains("Resultat pret"),
                eq(Map.of("persona", "FINDEM_WORKER", "taskId", taskId.toString(), "taskType", "PROFILE_SUMMARY")));
        verify(activityService).logSystemEvent(eq(applicationId), eq(companyId), eq(EventType.TASK_DONE), anyMap());
    }

    @Test
    void execute_skipsWhenAiDisabled() {
        service.execute(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "PROFILE_SUMMARY");

        verify(aiClientService).isEnabled();
        verifyNoInteractions(taskRepository, applicationRepository, chatAnswerService);
    }

    private Task task(UUID taskId) {
        Task task = new Task();
        task.setId(taskId);
        task.setDescription("Resume court");
        task.setStatus(Task.Status.TODO);
        return task;
    }

    private Application application(UUID applicationId) {
        Candidate candidate = new Candidate();
        candidate.setFirstName("Ada");
        candidate.setLastName("Lovelace");

        Job job = new Job();
        job.setTitle("Developpeuse Java");

        Application application = new Application();
        application.setApplicationId(applicationId);
        application.setCandidate(candidate);
        application.setJob(job);
        return application;
    }

    private ChatAnswerAnalysisDTO analysis(UUID applicationId) {
        ChatAnswerAnalysisDTO analysis = new ChatAnswerAnalysisDTO(applicationId.toString());
        analysis.setMotivationLevel("HIGH");
        analysis.setTechnicalLevel("STRONG");
        analysis.setRecommendedAction("PRIORITY");
        return analysis;
    }
}
