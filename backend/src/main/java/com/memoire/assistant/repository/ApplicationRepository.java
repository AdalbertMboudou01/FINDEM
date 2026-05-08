package com.memoire.assistant.repository;

import com.memoire.assistant.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {
	List<Application> findByCandidate_CandidateId(UUID candidateId);
	List<Application> findByJob_JobId(UUID jobId);
	List<Application> findByJob_Company_CompanyId(UUID companyId);
	Optional<Application> findByApplicationIdAndJob_Company_CompanyId(UUID applicationId, UUID companyId);
	Optional<Application> findByCandidate_CandidateIdAndJob_JobId(UUID candidateId, UUID jobId);
}
