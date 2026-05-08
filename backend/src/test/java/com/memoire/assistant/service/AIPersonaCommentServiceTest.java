package com.memoire.assistant.service;

import com.memoire.assistant.model.AIPersona;
import com.memoire.assistant.model.Application;
import com.memoire.assistant.model.ApplicationActivity.EventType;
import com.memoire.assistant.model.Comment;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.repository.CommentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIPersonaCommentServiceTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private ApplicationActivityService activityService;

    @InjectMocks private AIPersonaCommentService service;

    @Test
    void postInternalComment_createsAiSystemCommentAndSystemActivity() {
        UUID applicationId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Application application = new Application();
        application.setApplicationId(applicationId);

        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Comment result = service.postInternalComment(
                applicationId,
                companyId,
                AIPersona.FINDEM_ASSIST_ID,
                "Analyse IA",
                Map.of("persona", "FINDEM_ASSIST"));

        assertNotNull(result);
        assertEquals(application, result.getApplication());
        assertEquals(companyId, result.getCompanyId());
        assertEquals(AIPersona.FINDEM_ASSIST_ID, result.getAuthorId());
        assertEquals(Comment.AuthorType.AI_SYSTEM, result.getAuthorType());
        assertEquals(Comment.Visibility.INTERNAL, result.getVisibility());
        assertTrue(result.getMentions().isEmpty());

        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        verify(activityService).logSystemEvent(eq(applicationId), eq(companyId), eq(EventType.COMMENT_ADDED), payloadCaptor.capture());
        assertEquals("AI_SYSTEM", payloadCaptor.getValue().get("author_type"));
        assertEquals("FINDEM_ASSIST", payloadCaptor.getValue().get("persona"));
    }
}
