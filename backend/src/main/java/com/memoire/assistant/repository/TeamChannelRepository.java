package com.memoire.assistant.repository;

import com.memoire.assistant.model.TeamChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamChannelRepository extends JpaRepository<TeamChannel, UUID> {
    List<TeamChannel> findByCompanyIdAndTypeIn(UUID companyId, List<String> types);
    Optional<TeamChannel> findByCompanyIdAndType(UUID companyId, String type);
    Optional<TeamChannel> findByCompanyIdAndJobId(UUID companyId, UUID jobId);
    Optional<TeamChannel> findByCompanyIdAndRecruiterIdAndType(UUID companyId, UUID recruiterId, String type);
}
