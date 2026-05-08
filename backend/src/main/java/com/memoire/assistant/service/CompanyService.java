package com.memoire.assistant.service;

import com.memoire.assistant.model.Company;
import com.memoire.assistant.model.Recruiter;
import com.memoire.assistant.model.Job;
import com.memoire.assistant.dto.CompanyCreateRequest;
import com.memoire.assistant.dto.CompanyStatsDTO;
import com.memoire.assistant.repository.CompanyRepository;
import com.memoire.assistant.repository.RecruiterRepository;
import com.memoire.assistant.repository.JobRepository;
import com.memoire.assistant.validator.CompanyValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.Date;

@Service
@Transactional
public class CompanyService {
    @Autowired
    private CompanyRepository companyRepository;
    
    @Autowired
    private RecruiterRepository recruiterRepository;
    
    @Autowired
    private CompanyValidator companyValidator;
    
    @Autowired
    private JobRepository jobRepository;

    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    public Optional<Company> getCompanyById(UUID id) {
        return companyRepository.findById(id);
    }
    
    public Optional<Company> getCompanyWithStats(UUID id) {
        Optional<Company> company = companyRepository.findById(id);
        if (company.isPresent()) {
            enrichCompanyWithStats(company.get());
        }
        return company;
    }

    public Company saveCompany(Company company) {
        validateCompany(company);
        if (company.getCompanyId() == null) {
            company.setCreatedAt(new Date());
        }
        return companyRepository.save(company);
    }
    
    private void validateCompany(Company company) {
        if (company == null) {
            throw new IllegalArgumentException("L'entreprise ne peut pas être nulle");
        }
        if (company.getName() == null || company.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de l'entreprise est obligatoire");
        }
        if (company.getName().length() > 100) {
            throw new IllegalArgumentException("Le nom de l'entreprise ne peut pas dépasser 100 caractères");
        }
        if (company.getWebsite() != null && !company.getWebsite().trim().isEmpty()) {
            if (!company.getWebsite().startsWith("http://") && !company.getWebsite().startsWith("https://")) {
                company.setWebsite("https://" + company.getWebsite());
            }
        }
    }

    public Company createCompanyFromRequest(CompanyCreateRequest request) {
        companyValidator.validate(request);
        
        Company company = new Company();
        company.setName(request.getName());
        company.setSector(request.getSector());
        company.setSize(request.getSize());
        company.setWebsite(request.getWebsite());
        company.setPlan(request.getPlan());
        company.setCreatedAt(new Date());
        
        return saveCompany(company);
    }

    public void deleteCompany(UUID id) {
        // Vérifier s'il y a des recruteurs ou des offres associées
        List<Recruiter> recruiters = recruiterRepository.findByCompany_CompanyId(id);
        List<Job> jobs = jobRepository.findByCompany_CompanyId(id);
        
        if (!recruiters.isEmpty() || !jobs.isEmpty()) {
            throw new IllegalStateException("Impossible de supprimer l'entreprise : des recruteurs ou des offres d'emploi sont associés");
        }
        
        companyRepository.deleteById(id);
    }

    public List<Recruiter> getCompanyRecruiters(UUID companyId) {
        return recruiterRepository.findByCompany_CompanyId(companyId);
    }

    public List<Job> getCompanyJobs(UUID companyId) {
        return jobRepository.findByCompany_CompanyId(companyId);
    }

    public CompanyStatsDTO getCompanyStatistics(UUID companyId) {
        Optional<Company> company = companyRepository.findById(companyId);
        if (!company.isPresent()) {
            throw new IllegalArgumentException("Entreprise non trouvée");
        }
        
        List<Recruiter> recruiters = recruiterRepository.findByCompany_CompanyId(companyId);
        List<Job> jobs = jobRepository.findByCompany_CompanyId(companyId);
        
        CompanyStatsDTO stats = new CompanyStatsDTO();
        stats.setCompanyId(companyId);
        stats.setCompanyName(company.get().getName());
        stats.setRecruiterCount(recruiters.size());
        stats.setActiveJobCount(jobs.stream()
            .mapToInt(job -> 0) // Simplifié temporairement
            .sum());
        stats.setTotalJobCount(jobs.size());
        stats.setCreatedAt(company.get().getCreatedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        
        return stats;
    }

    public List<Company> searchCompanies(String keyword, String sector, String size) {
        if (keyword == null && sector == null && size == null) {
            return getAllCompanies();
        }
        
        return companyRepository.findAll().stream()
            .filter(company -> keyword == null || 
                company.getName().toLowerCase().contains(keyword.toLowerCase()) ||
                (company.getWebsite() != null && company.getWebsite().toLowerCase().contains(keyword.toLowerCase())))
            .filter(company -> sector == null || 
                (company.getSector() != null && company.getSector().equalsIgnoreCase(sector)))
            .filter(company -> size == null || 
                (company.getSize() != null && company.getSize().equalsIgnoreCase(size)))
            .collect(Collectors.toList());
    }
    
    private void enrichCompanyWithStats(Company company) {
        // Enrichir l'objet Company avec des statistiques si nécessaire
        // Cette méthode peut être étendue pour ajouter des données calculées
    }
}
