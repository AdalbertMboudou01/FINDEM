package com.memoire.assistant.controller;

import com.memoire.assistant.model.ChatbotQuestion;
import com.memoire.assistant.service.ChatbotQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs/{jobId}/questions")
public class ChatbotQuestionController {
    @Autowired
    private ChatbotQuestionService chatbotQuestionService;

    @GetMapping
    public List<ChatbotQuestion> getQuestions(@PathVariable UUID jobId) {
        return chatbotQuestionService.getQuestionsForJob(jobId);
    }

    @PostMapping
    public ChatbotQuestion addQuestion(@PathVariable UUID jobId, @RequestBody ChatbotQuestion question) {
        return chatbotQuestionService.addQuestion(
                jobId,
                question.getQuestionText(),
                question.getOrderIndex(),
                question.getAnswerType() != null ? question.getAnswerType() : "open",
                question.isRequired()
        );
    }

    @PutMapping("/{questionId}")
    public ResponseEntity<ChatbotQuestion> updateQuestion(@PathVariable UUID jobId, @PathVariable UUID questionId, @RequestBody ChatbotQuestion question) {
        Optional<ChatbotQuestion> updated = chatbotQuestionService.updateQuestion(
                questionId,
                question.getQuestionText(),
                question.getOrderIndex(),
                question.getAnswerType() != null ? question.getAnswerType() : "open",
                question.isRequired()
        );
        return updated.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable UUID jobId, @PathVariable UUID questionId) {
        chatbotQuestionService.deleteQuestion(questionId);
        return ResponseEntity.noContent().build();
    }
}
