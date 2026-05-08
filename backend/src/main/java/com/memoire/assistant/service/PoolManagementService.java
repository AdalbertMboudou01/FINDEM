package com.memoire.assistant.service;

import com.memoire.assistant.dto.CandidateSummaryDTO;
import com.memoire.assistant.model.Candidate;
import com.memoire.assistant.model.Application;
import com.memoire.assistant.repository.CandidateRepository;
import com.memoire.assistant.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PoolManagementService {
    
    @Autowired
    private CandidateRepository candidateRepository;
    
    @Autowired
    private ApplicationRepository applicationRepository;
    
    public Candidate addToPool(UUID candidateId, String reason) {
        Candidate candidate = candidateRepository.findById(candidateId)
            .orElseThrow(() -> new RuntimeException("Candidat non trouvé"));
        
        candidate.setInPool(true);
        return candidateRepository.save(candidate);
    }
    
    public Candidate removeFromPool(UUID candidateId) {
        Candidate candidate = candidateRepository.findById(candidateId)
            .orElseThrow(() -> new RuntimeException("Candidat non trouvé"));
        
        candidate.setInPool(false);
        return candidateRepository.save(candidate);
    }
    
    public List<CandidateSummaryDTO> getPoolCandidates() {
        List<Candidate> poolCandidates = candidateRepository.findByInPoolTrue();
        
        return poolCandidates.stream()
            .map(candidate -> {
                List<Application> applications = applicationRepository.findByCandidate_CandidateId(candidate.getCandidateId());
                return createCandidateSummary(candidate, applications);
            })
            .collect(Collectors.toList());
    }
    
    public List<CandidateSummaryDTO> searchPoolCandidates(String keyword, String skill, String location) {
        List<Candidate> poolCandidates = candidateRepository.findByInPoolTrue();
        
        return poolCandidates.stream()
            .filter(candidate -> matchesSearchCriteria(candidate, keyword, skill, location))
            .map(candidate -> {
                List<Application> applications = applicationRepository.findByCandidate_CandidateId(candidate.getCandidateId());
                return createCandidateSummary(candidate, applications);
            })
            .collect(Collectors.toList());
    }
    
    private boolean matchesSearchCriteria(Candidate candidate, String keyword, String skill, String location) {
        boolean matches = true;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            String lowerKeyword = keyword.toLowerCase();
            String candidateInfo = (candidate.getFirstName() + " " + candidate.getLastName() + " " + 
                                  candidate.getEmail() + " " + candidate.getSchool()).toLowerCase();
            matches = matches && candidateInfo.contains(lowerKeyword);
        }
        
        if (skill != null && !skill.trim().isEmpty()) {
            // Pour l'instant, on vérifie dans les URLs GitHub/portfolio
            // À améliorer avec une recherche dans les compétences extraites
            String lowerSkill = skill.toLowerCase();
            String urls = (candidate.getGithubUrl() + " " + candidate.getPortfolioUrl()).toLowerCase();
            matches = matches && urls.contains(lowerSkill);
        }
        
        if (location != null && !location.trim().isEmpty()) {
            String lowerLocation = location.toLowerCase();
            String candidateLocation = candidate.getLocation() != null ? candidate.getLocation().toLowerCase() : "";
            matches = matches && candidateLocation.contains(lowerLocation);
        }
        
        return matches;
    }
    
    public List<CandidateSummaryDTO> getPoolCandidatesBySkill(String skill) {
        return searchPoolCandidates(null, skill, null);
    }
    
    public List<CandidateSummaryDTO> getPoolCandidatesByLocation(String location) {
        return searchPoolCandidates(null, null, location);
    }
    
    public List<CandidateSummaryDTO> getRecentlyAddedPoolCandidates(int days) {
        // Cette méthode nécessiterait un champ "addedToPoolAt" dans l'entité Candidate
        // Pour l'instant, on retourne tous les candidats du vivier
        return getPoolCandidates();
    }
    
    public void reactivateCandidateForJob(UUID candidateId, UUID jobId) {
        // Cette méthode pourrait être utilisée pour réactiver un candidat du vivier
        // pour une nouvelle offre d'emploi
        Candidate candidate = candidateRepository.findById(candidateId)
            .orElseThrow(() -> new RuntimeException("Candidat non trouvé"));
        
        // Logique de réactivation à implémenter
        // Par exemple: créer une nouvelle application ou notifier le recruteur
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
                .filter(status -> status != null)
                .findFirst()
                .orElse(null);
        dto.setLastStatus(lastStatus);
        
        return dto;
    }
    
    public PoolStatistics getPoolStatistics() {
        List<Candidate> poolCandidates = candidateRepository.findByInPoolTrue();
        
        PoolStatistics stats = new PoolStatistics();
        stats.setTotalCandidates(poolCandidates.size());
        
        // Statistiques par localisation
        stats.setCandidatesByLocation(
            poolCandidates.stream()
                .filter(c -> c.getLocation() != null)
                .collect(Collectors.groupingBy(
                    c -> c.getLocation(),
                    Collectors.counting()
                ))
        );
        
        // Statistiques par école
        stats.setCandidatesBySchool(
            poolCandidates.stream()
                .filter(c -> c.getSchool() != null)
                .collect(Collectors.groupingBy(
                    c -> c.getSchool(),
                    Collectors.counting()
                ))
        );
        
        // Statistiques des candidats avec GitHub
        long withGithub = poolCandidates.stream()
            .filter(c -> c.getGithubUrl() != null && !c.getGithubUrl().trim().isEmpty())
            .count();
        stats.setCandidatesWithGithub(withGithub);
        stats.setCandidatesWithPortfolio(
            poolCandidates.stream()
                .filter(c -> c.getPortfolioUrl() != null && !c.getPortfolioUrl().trim().isEmpty())
                .count()
        );
        
        return stats;
    }
    
    public static class PoolStatistics {
        private int totalCandidates;
        private java.util.Map<String, Long> candidatesByLocation;
        private java.util.Map<String, Long> candidatesBySchool;
        private long candidatesWithGithub;
        private long candidatesWithPortfolio;
        
        // Getters & Setters
        public int getTotalCandidates() {
            return totalCandidates;
        }
        
        public void setTotalCandidates(int totalCandidates) {
            this.totalCandidates = totalCandidates;
        }
        
        public java.util.Map<String, Long> getCandidatesByLocation() {
            return candidatesByLocation;
        }
        
        public void setCandidatesByLocation(java.util.Map<String, Long> candidatesByLocation) {
            this.candidatesByLocation = candidatesByLocation;
        }
        
        public java.util.Map<String, Long> getCandidatesBySchool() {
            return candidatesBySchool;
        }
        
        public void setCandidatesBySchool(java.util.Map<String, Long> candidatesBySchool) {
            this.candidatesBySchool = candidatesBySchool;
        }
        
        public long getCandidatesWithGithub() {
            return candidatesWithGithub;
        }
        
        public void setCandidatesWithGithub(long candidatesWithGithub) {
            this.candidatesWithGithub = candidatesWithGithub;
        }
        
        public long getCandidatesWithPortfolio() {
            return candidatesWithPortfolio;
        }
        
        public void setCandidatesWithPortfolio(long candidatesWithPortfolio) {
            this.candidatesWithPortfolio = candidatesWithPortfolio;
        }
    }
}
