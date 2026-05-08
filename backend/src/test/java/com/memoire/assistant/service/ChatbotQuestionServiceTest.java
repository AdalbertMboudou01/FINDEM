package com.memoire.assistant.service;

import com.memoire.assistant.model.ChatbotQuestion;
import com.memoire.assistant.model.Job;
import com.memoire.assistant.repository.ChatbotQuestionRepository;
import com.memoire.assistant.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatbotQuestionServiceTest {
    @Mock
    private ChatbotQuestionRepository chatbotQuestionRepository;
    @Mock
    private JobRepository jobRepository;
    @InjectMocks
    private ChatbotQuestionService chatbotQuestionService;

    private UUID jobId;
    private Job job;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        jobId = UUID.randomUUID();
        job = new Job();
        job.setJobId(jobId);
    }

    @Test
    void testAddAndGetQuestions() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        ChatbotQuestion q = new ChatbotQuestion();
        q.setJob(job);
        q.setQuestionText("Votre motivation ?");
        q.setOrderIndex(1);
        q.setAnswerType("open");
        q.setRequired(true);
        when(chatbotQuestionRepository.save(any())).thenReturn(q);
        when(chatbotQuestionRepository.findByJobOrderByOrderIndexAsc(job)).thenReturn(List.of(q));

        ChatbotQuestion created = chatbotQuestionService.addQuestion(jobId, "Votre motivation ?", 1, "open", true);
        assertEquals("Votre motivation ?", created.getQuestionText());
        assertEquals("open", created.getAnswerType());
        assertTrue(created.isRequired());
        List<ChatbotQuestion> questions = chatbotQuestionService.getQuestionsForJob(jobId);
        assertEquals(1, questions.size());
        assertEquals("Votre motivation ?", questions.get(0).getQuestionText());
    }

    @Test
    void testUpdateQuestion() {
        UUID qId = UUID.randomUUID();
        ChatbotQuestion q = new ChatbotQuestion();
        q.setId(qId);
        q.setQuestionText("Ancienne question");
        q.setOrderIndex(1);
        q.setAnswerType("open");
        q.setRequired(true);
        when(chatbotQuestionRepository.findById(qId)).thenReturn(Optional.of(q));
        when(chatbotQuestionRepository.save(any())).thenReturn(q);

        Optional<ChatbotQuestion> updated = chatbotQuestionService.updateQuestion(qId, "Nouvelle question", 2, "boolean", false);
        assertTrue(updated.isPresent());
        assertEquals("Nouvelle question", updated.get().getQuestionText());
        assertEquals(2, updated.get().getOrderIndex());
        assertEquals("boolean", updated.get().getAnswerType());
        assertFalse(updated.get().isRequired());
    }

    @Test
    void testDeleteQuestion() {
        UUID qId = UUID.randomUUID();
        doNothing().when(chatbotQuestionRepository).deleteById(qId);
        chatbotQuestionService.deleteQuestion(qId);
        verify(chatbotQuestionRepository, times(1)).deleteById(qId);
    }
}
