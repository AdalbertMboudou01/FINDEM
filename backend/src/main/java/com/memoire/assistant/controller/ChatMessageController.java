package com.memoire.assistant.controller;

import com.memoire.assistant.dto.ChatAnswerDTO;
import com.memoire.assistant.dto.ChatAnswerResponse;
import com.memoire.assistant.model.ChatMessage;
import com.memoire.assistant.service.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat Messages", description = "Gestion des messages du chatbot")
public class ChatMessageController {
    
    @Autowired
    private ChatMessageService chatMessageService;
    
    @PostMapping("/answer")
    @Operation(summary = "Sauvegarder une réponse du chatbot", description = "Permet de sauvegarder la réponse d'un candidat à une question du chatbot")
    public ResponseEntity<?> saveAnswer(@Valid @RequestBody ChatAnswerDTO answerDTO) {
        try {
            ChatMessage savedMessage = chatMessageService.saveAnswer(answerDTO);
            ChatAnswerResponse response = new ChatAnswerResponse();
            response.setMessageId(savedMessage.getMessageId());
            response.setApplicationId(savedMessage.getApplication() != null ? savedMessage.getApplication().getApplicationId() : null);
            response.setCandidateId(savedMessage.getCandidate() != null ? savedMessage.getCandidate().getCandidateId() : null);
            response.setQuestionKey(savedMessage.getQuestionKey());
            response.setQuestionText(savedMessage.getQuestionText());
            response.setAnswer(savedMessage.getAnswer());
            response.setMessageType(savedMessage.getMessageType());
            response.setCreatedAt(savedMessage.getCreatedAt());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            String message = e.getMessage() == null || e.getMessage().isBlank()
                ? "Reponse invalide ou trop longue"
                : e.getMessage();
            return ResponseEntity.badRequest().body(Map.of("message", message));
        }
    }
    
    @GetMapping("/application/{applicationId}")
    @Operation(summary = "Récupérer les messages d'une candidature", description = "Retourne tous les messages (questions et réponses) pour une candidature donnée")
    public ResponseEntity<List<ChatMessage>> getMessagesByApplication(@PathVariable UUID applicationId) {
        List<ChatMessage> messages = chatMessageService.getMessagesByApplication(applicationId);
        return ResponseEntity.ok(messages);
    }
    
    @GetMapping("/application/{applicationId}/answers")
    @Operation(summary = "Récupérer les réponses d'une candidature", description = "Retourne uniquement les réponses du candidat pour une candidature donnée")
    public ResponseEntity<List<ChatMessage>> getAnswersByApplication(@PathVariable UUID applicationId) {
        List<ChatMessage> answers = chatMessageService.getAnswersByApplication(applicationId);
        return ResponseEntity.ok(answers);
    }
    
    @GetMapping("/candidate/{candidateId}")
    @Operation(summary = "Récupérer les messages d'un candidat", description = "Retourne tous les messages d'un candidat")
    public ResponseEntity<List<ChatMessage>> getMessagesByCandidate(@PathVariable UUID candidateId) {
        List<ChatMessage> messages = chatMessageService.getMessagesByCandidate(candidateId);
        return ResponseEntity.ok(messages);
    }
    
    @DeleteMapping("/application/{applicationId}")
    @Operation(summary = "Supprimer les messages d'une candidature", description = "Supprime tous les messages associés à une candidature")
    public ResponseEntity<Void> deleteMessagesByApplication(@PathVariable UUID applicationId) {
        chatMessageService.deleteMessagesByApplication(applicationId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/application/{applicationId}/complete")
    @Operation(summary = "Vérifier si toutes les réponses requises sont fournies", description = "Vérifie si le candidat a répondu à toutes les questions requises")
    public ResponseEntity<Boolean> checkCompletionStatus(
            @PathVariable UUID applicationId,
            @RequestParam List<String> requiredQuestions) {
        boolean isComplete = chatMessageService.hasAllRequiredAnswers(applicationId, requiredQuestions);
        return ResponseEntity.ok(isComplete);
    }
}
