package com.memoire.assistant.repository;

import com.memoire.assistant.model.ChatbotQuestion;
import com.memoire.assistant.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatbotQuestionRepository extends JpaRepository<ChatbotQuestion, UUID> {
    List<ChatbotQuestion> findByJobOrderByOrderIndexAsc(Job job);
}
