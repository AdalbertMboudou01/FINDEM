package com.memoire.assistant.repository;

import com.memoire.assistant.model.ChatQuestionConfig;
import com.memoire.assistant.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatQuestionConfigRepository extends JpaRepository<ChatQuestionConfig, UUID> {
    
    List<ChatQuestionConfig> findByJob_JobId(UUID jobId);
    
    List<ChatQuestionConfig> findByJob_JobIdOrderByOrderIndexAsc(UUID jobId);
    
    @Query("SELECT c FROM ChatQuestionConfig c WHERE c.job.jobId = :jobId AND c.required = true")
    List<ChatQuestionConfig> findRequiredQuestionsByJobId(@Param("jobId") UUID jobId);
    
    @Query("SELECT c FROM ChatQuestionConfig c WHERE c.job.jobId = :jobId AND c.adaptiveEnabled = true")
    List<ChatQuestionConfig> findAdaptiveQuestionsByJobId(@Param("jobId") UUID jobId);
    
    void deleteByJob_JobId(UUID jobId);
}
