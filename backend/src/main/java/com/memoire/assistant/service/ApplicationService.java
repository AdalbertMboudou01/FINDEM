package com.memoire.assistant.service;

import com.memoire.assistant.model.Application;
import com.memoire.assistant.repository.ApplicationRepository;
import com.memoire.assistant.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApplicationService {
    @Autowired
    private ApplicationRepository applicationRepository;

    public List<Application> getAllApplications() {
        return applicationRepository.findAll();
    }

    public List<Application> getAllApplicationsForCurrentCompany() {
        return applicationRepository.findByJob_Company_CompanyId(requireCompanyId());
    }

    public Optional<Application> getApplicationById(UUID id) {
        return applicationRepository.findById(id);
    }

    public Optional<Application> getApplicationByIdForCurrentCompany(UUID id) {
        return applicationRepository.findByApplicationIdAndJob_Company_CompanyId(id, requireCompanyId());
    }

    public Application saveApplication(Application application) {
        return applicationRepository.save(application);
    }

    public void deleteApplication(UUID id) {
        applicationRepository.deleteById(id);
    }

    private UUID requireCompanyId() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new IllegalStateException("Contexte entreprise manquant");
        }
        return companyId;
    }
}
