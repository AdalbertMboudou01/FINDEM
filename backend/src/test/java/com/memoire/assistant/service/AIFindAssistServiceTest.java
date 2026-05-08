package com.memoire.assistant.service;

import com.memoire.assistant.dto.ChatAnswerAnalysisDTO;
import com.memoire.assistant.model.AIPersona;
import com.memoire.assistant.model.Application;
import com.memoire.assistant.model.Company;
import com.memoire.assistant.model.Job;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIFindAssistServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private AIPersonaCommentService aiPersonaCommentService;

    @InjectMocks private AIFindAssistService service;

    @Test
    void postAnalysisIfNeeded_postsInitialAnalysisOnce() {
        UUID applicationId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Application application = applicationWithCompany(applicationId, companyId);

        when(commentRepository.existsByApplication_ApplicationIdAndAuthorId(applicationId, AIPersona.FINDEM_ASSIST_ID))
                .thenReturn(false);
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        service.postAnalysisIfNeeded(applicationId, analysis());

        verify(aiPersonaCommentService).postInternalComment(
                eq(applicationId),
                eq(companyId),
                eq(AIPersona.FINDEM_ASSIST_ID),
                contains("FindemAssist"),
                eq(Map.of("persona", "FINDEM_ASSIST")));
    }

    @Test
    void postAnalysisIfNeeded_skipsWhenAssistAlreadyPosted() {
        UUID applicationId = UUID.randomUUID();
        when(commentRepository.existsByApplication_ApplicationIdAndAuthorId(applicationId, AIPersona.FINDEM_ASSIST_ID))
                .thenReturn(true);

        service.postAnalysisIfNeeded(applicationId, analysis());

        verifyNoInteractions(applicationRepository, aiPersonaCommentService);
    }

    private Application applicationWithCompany(UUID applicationId, UUID companyId) {
        Company company = new Company();
        company.setCompanyId(companyId);

        Job job = new Job();
        job.setCompany(company);

        Application application = new Application();
        application.setApplicationId(applicationId);
        application.setJob(job);
        return application;
    }

    private ChatAnswerAnalysisDTO analysis() {
        ChatAnswerAnalysisDTO analysis = new ChatAnswerAnalysisDTO(UUID.randomUUID().toString());
        analysis.setMotivationLevel("HIGH");
        analysis.setTechnicalLevel("STRONG");
        analysis.setRecommendedAction("PRIORITY");
        return analysis;
    }
}
