package com.memoire.assistant.repository;

import com.memoire.assistant.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface CandidateRepository extends JpaRepository<Candidate, UUID> {
	java.util.List<Candidate> findByInPoolTrue();
	java.util.Optional<Candidate> findByEmail(String email);

	@Query("select distinct c from Candidate c join Application a on a.candidate = c where a.job.company.companyId = :companyId")
	List<Candidate> findAllByCompanyId(@Param("companyId") UUID companyId);

	@Query("select distinct c from Candidate c join Application a on a.candidate = c where c.candidateId = :candidateId and a.job.company.companyId = :companyId")
	Optional<Candidate> findByCandidateIdAndCompanyId(@Param("candidateId") UUID candidateId, @Param("companyId") UUID companyId);

	@Query("select distinct c from Candidate c join Application a on a.candidate = c where c.inPool = true and a.job.company.companyId = :companyId")
	List<Candidate> findInPoolByCompanyId(@Param("companyId") UUID companyId);
}
