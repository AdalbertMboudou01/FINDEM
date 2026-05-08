package com.memoire.assistant.repository;

import com.memoire.assistant.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
    Optional<Job> findBySlug(String slug);
    List<Job> findByCompany_CompanyId(UUID companyId);
    Optional<Job> findByJobIdAndCompany_CompanyId(UUID jobId, UUID companyId);
}
