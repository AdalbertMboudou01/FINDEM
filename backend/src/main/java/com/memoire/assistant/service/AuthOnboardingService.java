package com.memoire.assistant.service;

import com.memoire.assistant.dto.AuthResponse;
import com.memoire.assistant.dto.RegisterCompanyOwnerRequest;
import com.memoire.assistant.model.Company;
import com.memoire.assistant.model.Recruiter;
import com.memoire.assistant.model.User;
import com.memoire.assistant.repository.CompanyRepository;
import com.memoire.assistant.repository.RecruiterRepository;
import com.memoire.assistant.repository.UserRepository;
import com.memoire.assistant.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Date;

@Service
public class AuthOnboardingService {
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RecruiterRepository recruiterRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthOnboardingService(
        UserRepository userRepository,
        CompanyRepository companyRepository,
        RecruiterRepository recruiterRepository,
        PasswordEncoder passwordEncoder,
        JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.recruiterRepository = recruiterRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponse registerCompanyOwner(RegisterCompanyOwnerRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les mots de passe ne correspondent pas");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cet email est deja utilise");
        }

        User user = new User();
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ADMIN");
        user = userRepository.save(user);

        Company company = new Company();
        company.setName(request.getCompanyName());
        company.setSector(request.getSector());
        company.setSize(request.getSize());
        company.setWebsite(request.getWebsite());
        company.setPlan(request.getPlan() != null && !request.getPlan().isBlank() ? request.getPlan() : "starter");
        company.setConfig(request.getConfig());
        company.setCreatedAt(new Date());
        company = companyRepository.save(company);

        Recruiter recruiter = new Recruiter();
        recruiter.setCompany(company);
        recruiter.setEmail(user.getEmail());
        recruiter.setName(request.getFullName());
        recruiter.setRole("ADMIN");
        recruiter.setStatus("active");
        recruiter.setAuthUserId(user.getId());
        recruiter = recruiterRepository.save(recruiter);

        String token = jwtUtil.generateToken(
            user.getEmail(),
            user.getRole(),
            user.getId(),
            recruiter.getRecruiterId(),
            company.getCompanyId()
        );

        return new AuthResponse(
            token,
            user.getRole(),
            user.getId(),
            recruiter.getRecruiterId(),
            company.getCompanyId(),
            true
        );
    }
}
