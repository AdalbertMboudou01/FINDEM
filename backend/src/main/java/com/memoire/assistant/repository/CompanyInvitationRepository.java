package com.memoire.assistant.repository;

import com.memoire.assistant.model.CompanyInvitation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyInvitationRepository extends JpaRepository<CompanyInvitation, UUID> {
    List<CompanyInvitation> findByCompany_CompanyIdOrderByCreatedAtDesc(UUID companyId);
    Optional<CompanyInvitation> findByInvitationToken(String invitationToken);
    Optional<CompanyInvitation> findByCompany_CompanyIdAndEmailIgnoreCaseAndStatus(
        UUID companyId,
        String email,
        CompanyInvitation.Status status
    );
}
