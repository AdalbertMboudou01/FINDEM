package com.memoire.assistant.repository;

import com.memoire.assistant.model.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationStatusRepository extends JpaRepository<ApplicationStatus, UUID> {
    Optional<ApplicationStatus> findByCode(String code);
}
