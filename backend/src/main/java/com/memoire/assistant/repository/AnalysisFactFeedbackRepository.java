package com.memoire.assistant.repository;

import com.memoire.assistant.model.AnalysisFactFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AnalysisFactFeedbackRepository extends JpaRepository<AnalysisFactFeedback, UUID> {

    List<AnalysisFactFeedback> findByApplication_ApplicationIdOrderByCreatedAtDesc(UUID applicationId);

    List<AnalysisFactFeedback> findByApplication_Job_Company_CompanyIdAndCreatedAtAfterOrderByCreatedAtDesc(UUID companyId, LocalDateTime createdAt);

    List<AnalysisFactFeedback> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime createdAt);
}
