package com.memoire.assistant.controller;

import com.memoire.assistant.dto.ChatbotQuestionDTO;
import com.memoire.assistant.service.ChatbotQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {
    @Autowired
    private ChatbotQuestionService chatbotQuestionService;

    @GetMapping("/questions")
    public List<ChatbotQuestionDTO> getQuestions(@RequestParam UUID jobId) {
        return chatbotQuestionService.getQuestionsForJob(jobId).stream()
                .map(q -> new ChatbotQuestionDTO(
                        q.getId() != null ? q.getId().toString() : null,
                        q.getQuestionText(),
                        "text")) // ou "textarea" selon le besoin
                .toList();
    }
}
