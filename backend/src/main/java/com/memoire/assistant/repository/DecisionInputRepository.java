package com.memoire.assistant.repository;

import com.memoire.assistant.model.DecisionInput;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DecisionInputRepository extends JpaRepository<DecisionInput, UUID> {
    List<DecisionInput> findByApplicationIdAndCompanyIdOrderByCreatedAtAsc(UUID applicationId, UUID companyId);
    boolean existsByApplicationIdAndAuthorIdAndCompanyId(UUID applicationId, UUID authorId, UUID companyId);

    @Query("SELECT COUNT(d) FROM DecisionInput d WHERE d.applicationId = :applicationId")
    long countByApplicationId(@Param("applicationId") UUID applicationId);
}
