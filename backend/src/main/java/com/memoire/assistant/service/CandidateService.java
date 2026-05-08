
package com.memoire.assistant.service;

import com.memoire.assistant.model.Candidate;
import com.memoire.assistant.repository.CandidateRepository;
import com.memoire.assistant.model.Application;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.dto.CandidateSummaryDTO;
import com.memoire.assistant.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Objects;

@Service
public class CandidateService {
    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    public List<Candidate> getAllCandidates() {
        return candidateRepository.findAllByCompanyId(requireCompanyId());
    }

    public Optional<Candidate> getCandidateById(UUID id) {
        return candidateRepository.findByCandidateIdAndCompanyId(id, requireCompanyId());
    }

    public Candidate saveCandidate(Candidate candidate) {
        return candidateRepository.save(candidate);
    }

    public void deleteCandidate(UUID id) {
        Optional<Candidate> candidateOpt = candidateRepository.findByCandidateIdAndCompanyId(id, requireCompanyId());
        if (candidateOpt.isEmpty()) {
            throw new IllegalArgumentException("Candidat introuvable pour votre entreprise");
        }
        candidateRepository.deleteById(id);
    }

    public List<Candidate> getPoolCandidates() {
        return candidateRepository.findInPoolByCompanyId(requireCompanyId());
    }

    public Optional<CandidateSummaryDTO> getCandidateSummary(UUID id) {
        Optional<Candidate> candidateOpt = candidateRepository.findByCandidateIdAndCompanyId(id, requireCompanyId());
        if (candidateOpt.isEmpty()) return Optional.empty();
        Candidate candidate = candidateOpt.get();
        List<Application> applications = applicationRepository.findByCandidate_CandidateId(id);
        UUID companyId = requireCompanyId();
        applications = applications.stream()
            .filter(a -> a.getJob() != null && a.getJob().getCompany() != null && companyId.equals(a.getJob().getCompany().getCompanyId()))
            .toList();
        return Optional.of(createCandidateSummary(candidate, applications));
    }

    private CandidateSummaryDTO createCandidateSummary(Candidate candidate, List<Application> applications) {
        CandidateSummaryDTO dto = new CandidateSummaryDTO();
        dto.setCandidateId(candidate.getCandidateId());
        dto.setFirstName(candidate.getFirstName());
        dto.setLastName(candidate.getLastName());
        dto.setEmail(candidate.getEmail());
        dto.setInPool(candidate.getInPool());
        dto.setApplicationsCount(applications.size());
        String lastStatus = applications.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(a -> a.getStatus() != null ? a.getStatus().getLabel() : null)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
        dto.setLastStatus(lastStatus);
        return dto;
    }

    private UUID requireCompanyId() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new IllegalStateException("Contexte entreprise manquant");
        }
        return companyId;
    }
}
