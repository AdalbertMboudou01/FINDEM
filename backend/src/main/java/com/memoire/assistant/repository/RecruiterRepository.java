package com.memoire.assistant.repository;

import com.memoire.assistant.model.Recruiter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;

public interface RecruiterRepository extends JpaRepository<Recruiter, UUID> {
    java.util.List<Recruiter> findByCompany_CompanyId(UUID companyId);
    Optional<Recruiter> findByAuthUserId(UUID authUserId);
    Optional<Recruiter> findByRecruiterIdAndCompany_CompanyId(UUID recruiterId, UUID companyId);
    Optional<Recruiter> findByCompany_CompanyIdAndEmailIgnoreCase(UUID companyId, String email);
    Optional<Recruiter> findByAuthUserIdAndCompany_CompanyId(UUID authUserId, UUID companyId);
}
