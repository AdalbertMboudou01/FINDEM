package com.memoire.assistant.controller;

import com.memoire.assistant.model.Company;
import com.memoire.assistant.model.Recruiter;
import com.memoire.assistant.model.User;
import com.memoire.assistant.repository.RecruiterRepository;
import com.memoire.assistant.repository.UserRepository;
import com.memoire.assistant.security.TenantContext;
import com.memoire.assistant.service.CompanyService;
import com.memoire.assistant.dto.CompanyCreateRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Collections;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {
    @Value("${app.features.bootstrap-test-enabled:false}")
    private boolean bootstrapTestEnabled;

    @Autowired
    private CompanyService companyService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RecruiterRepository recruiterRepository;

    @GetMapping
    public List<Company> getAllCompanies() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            return Collections.emptyList();
        }
        return companyService.getCompanyById(companyId)
            .map(List::of)
            .orElseGet(Collections::emptyList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Company> getCompanyById(@PathVariable UUID id) {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null || !companyId.equals(id)) {
            return ResponseEntity.notFound().build();
        }
        Optional<Company> company = companyService.getCompanyById(id);
        return company.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Company createCompany(@Valid @RequestBody CompanyCreateRequest request) {
        Company company = new Company();
        company.setName(request.getName());
        company.setSector(request.getSector());
        company.setSize(request.getSize());
        company.setWebsite(request.getWebsite());
        company.setPlan(request.getPlan());
        company.setConfig(request.getConfig());
        return companyService.saveCompany(company);
    }

    @PostMapping("/bootstrap-test")
    public ResponseEntity<Map<String, Object>> bootstrapTestCompany(Authentication authentication) {
        if (!bootstrapTestEnabled) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Endpoint desactive"));
        }

        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Utilisateur introuvable"));
        }

        User user = userOpt.get();
        Optional<Recruiter> existingRecruiter = recruiterRepository.findByAuthUserId(user.getId());

        if (existingRecruiter.isPresent() && existingRecruiter.get().getCompany() != null) {
            Recruiter recruiter = existingRecruiter.get();
            Company company = recruiter.getCompany();

            Map<String, Object> response = new HashMap<>();
            response.put("created", false);
            response.put("companyId", company.getCompanyId());
            response.put("ownerRecruiterId", recruiter.getRecruiterId());
            response.put("companyName", company.getName());
            response.put("email", recruiter.getEmail());
            return ResponseEntity.ok(response);
        }

        String emailPrefix = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;

        Company company = new Company();
        company.setName("Entreprise test - " + emailPrefix);
        company.setSector("Technologie");
        company.setSize("1-10");
        company.setWebsite("https://exemple.local");
        company.setPlan("starter");
        company.setCreatedAt(new Date());
        company = companyService.saveCompany(company);

        Recruiter recruiter = existingRecruiter.orElseGet(Recruiter::new);
        recruiter.setCompany(company);
        recruiter.setAuthUserId(user.getId());
        recruiter.setEmail(email);
        recruiter.setName(emailPrefix);
        recruiter.setRole("ADMIN");
        recruiter.setStatus("active");
        recruiter = recruiterRepository.save(recruiter);

        Map<String, Object> response = new HashMap<>();
        response.put("created", true);
        response.put("companyId", company.getCompanyId());
        response.put("ownerRecruiterId", recruiter.getRecruiterId());
        response.put("companyName", company.getName());
        response.put("email", recruiter.getEmail());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Company> updateCompany(@PathVariable UUID id, @RequestBody Company company) {
        UUID tenantCompanyId = TenantContext.getCompanyId();
        if (tenantCompanyId == null || !tenantCompanyId.equals(id)) {
            return ResponseEntity.notFound().build();
        }
        if (!TenantContext.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!companyService.getCompanyById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        company.setCompanyId(id);
        return ResponseEntity.ok(companyService.saveCompany(company));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(@PathVariable UUID id) {
        UUID tenantCompanyId = TenantContext.getCompanyId();
        if (tenantCompanyId == null || !tenantCompanyId.equals(id)) {
            return ResponseEntity.notFound().build();
        }
        if (!TenantContext.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!companyService.getCompanyById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        companyService.deleteCompany(id);
        return ResponseEntity.noContent().build();
    }
}
