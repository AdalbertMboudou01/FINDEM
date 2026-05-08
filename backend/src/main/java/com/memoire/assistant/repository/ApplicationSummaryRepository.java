package com.memoire.assistant.repository;

import com.memoire.assistant.model.ApplicationSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationSummaryRepository extends JpaRepository<ApplicationSummary, UUID> {
    Optional<ApplicationSummary> findByApplication_ApplicationId(UUID applicationId);
    List<ApplicationSummary> findByRecommendedAction(String recommendedAction);
    List<ApplicationSummary> findByApplication_Job_JobId(UUID jobId);
    void deleteByApplication_ApplicationId(UUID applicationId);
}
