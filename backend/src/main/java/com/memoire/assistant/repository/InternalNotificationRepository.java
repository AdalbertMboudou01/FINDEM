package com.memoire.assistant.repository;

import com.memoire.assistant.model.InternalNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InternalNotificationRepository extends JpaRepository<InternalNotification, UUID> {

    List<InternalNotification> findByUserIdAndCompanyIdOrderByCreatedAtDesc(UUID userId, UUID companyId);

    long countByUserIdAndCompanyIdAndReadAtIsNull(UUID userId, UUID companyId);
}
