package com.memoire.assistant.repository;

import com.memoire.assistant.model.ApplicationActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationActivityRepository extends JpaRepository<ApplicationActivity, UUID> {

    List<ApplicationActivity> findByApplication_ApplicationIdOrderByCreatedAtAsc(UUID applicationId);

    List<ApplicationActivity> findByApplication_ApplicationIdAndCompanyIdOrderByCreatedAtAsc(UUID applicationId, UUID companyId);

    List<ApplicationActivity> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}
