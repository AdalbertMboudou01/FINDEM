package com.memoire.assistant.service;

import com.memoire.assistant.dto.AcceptInvitationRequest;
import com.memoire.assistant.dto.AuthResponse;
import com.memoire.assistant.dto.CompanyInvitationDTO;
import com.memoire.assistant.dto.InviteRecruiterRequest;
import com.memoire.assistant.model.Company;
import com.memoire.assistant.model.CompanyInvitation;
import com.memoire.assistant.model.Department;
import com.memoire.assistant.model.Recruiter;
import com.memoire.assistant.model.User;
import com.memoire.assistant.repository.CompanyInvitationRepository;
import com.memoire.assistant.repository.CompanyRepository;
import com.memoire.assistant.repository.DepartmentRepository;
import com.memoire.assistant.repository.RecruiterRepository;
import com.memoire.assistant.repository.UserRepository;
import com.memoire.assistant.security.JwtUtil;
import com.memoire.assistant.security.TenantContext;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CompanyInvitationService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CompanyInvitationRepository invitationRepository;
    private final CompanyRepository companyRepository;
    private final DepartmentRepository departmentRepository;
    private final RecruiterRepository recruiterRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SmtpMailService smtpMailService;

    @Value("${app.frontend.url:http://localhost:3100}")
    private String frontendUrl;

    public CompanyInvitationService(
        CompanyInvitationRepository invitationRepository,
        CompanyRepository companyRepository,
        DepartmentRepository departmentRepository,
        RecruiterRepository recruiterRepository,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtUtil jwtUtil,
        SmtpMailService smtpMailService
    ) {
        this.invitationRepository = invitationRepository;
        this.companyRepository = companyRepository;
        this.departmentRepository = departmentRepository;
        this.recruiterRepository = recruiterRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.smtpMailService = smtpMailService;
    }

    public List<CompanyInvitationDTO> listForCurrentCompany() {
        UUID companyId = requireAdminCompanyId();
        return invitationRepository.findByCompany_CompanyIdOrderByCreatedAtDesc(companyId)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional
    public CompanyInvitationDTO invite(InviteRecruiterRequest request) {
        UUID companyId = requireAdminCompanyId();
        String email = normalizeEmail(request.getEmail());
        String role = normalizeRole(request.getRole());

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un utilisateur existe deja avec cet email");
        }

        if (recruiterRepository.findByCompany_CompanyIdAndEmailIgnoreCase(companyId, email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce membre appartient deja a votre entreprise");
        }

        invitationRepository
            .findByCompany_CompanyIdAndEmailIgnoreCaseAndStatus(companyId, email, CompanyInvitation.Status.PENDING)
            .ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Une invitation est deja en attente pour cet email");
            });

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entreprise introuvable"));

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository
                .findByDepartmentIdAndCompany_CompanyId(request.getDepartmentId(), companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service introuvable"));
        }

        CompanyInvitation invitation = new CompanyInvitation();
        invitation.setCompany(company);
        invitation.setDepartment(department);
        invitation.setEmail(email);
        invitation.setRole(role);
        invitation.setInvitationToken(generateToken());
        invitation.setStatus(CompanyInvitation.Status.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        CompanyInvitation saved = invitationRepository.save(invitation);
        try {
            sendInvitationEmail(saved);
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
        return toDto(saved);
    }

    public CompanyInvitationDTO getByToken(String token) {
        CompanyInvitation invitation = invitationRepository.findByInvitationToken(token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation introuvable"));
        expireIfNeeded(invitation);
        return toDto(invitation);
    }

    @Transactional
    public AuthResponse accept(AcceptInvitationRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les mots de passe ne correspondent pas");
        }

        CompanyInvitation invitation = invitationRepository.findByInvitationToken(request.getToken())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation introuvable"));
        expireIfNeeded(invitation);

        if (invitation.getStatus() != CompanyInvitation.Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette invitation n'est plus active");
        }

        String email = invitation.getEmail();
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un utilisateur existe deja avec cet email");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(invitation.getRole());
        user = userRepository.save(user);

        Recruiter recruiter = new Recruiter();
        recruiter.setCompany(invitation.getCompany());
        recruiter.setDepartment(invitation.getDepartment());
        recruiter.setEmail(email);
        recruiter.setName(request.getFullName().trim());
        recruiter.setRole(invitation.getRole());
        recruiter.setStatus("active");
        recruiter.setAuthUserId(user.getId());
        recruiter = recruiterRepository.save(recruiter);

        invitation.setStatus(CompanyInvitation.Status.ACCEPTED);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        String token = jwtUtil.generateToken(
            user.getEmail(),
            user.getRole(),
            user.getId(),
            recruiter.getRecruiterId(),
            invitation.getCompany().getCompanyId()
        );

        return new AuthResponse(
            token,
            user.getRole(),
            user.getId(),
            recruiter.getRecruiterId(),
            invitation.getCompany().getCompanyId(),
            true
        );
    }

    @Transactional
    public void cancel(UUID invitationId) {
        UUID companyId = requireAdminCompanyId();
        CompanyInvitation invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation introuvable"));
        if (invitation.getCompany() == null || !companyId.equals(invitation.getCompany().getCompanyId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invitation introuvable");
        }
        if (invitation.getStatus() == CompanyInvitation.Status.PENDING) {
            invitation.setStatus(CompanyInvitation.Status.CANCELLED);
            invitationRepository.save(invitation);
        }
    }

    private UUID requireAdminCompanyId() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Contexte entreprise manquant");
        }
        if (!TenantContext.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Reserve aux administrateurs");
        }
        return companyId;
    }

    private String normalizeRole(String role) {
        String value = role == null ? "" : role.trim().toUpperCase();
        if (!value.equals("RECRUITER") && !value.equals("MANAGER") && !value.equals("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role invalide. Roles autorises: RECRUITER, MANAGER, ADMIN");
        }
        return value;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private void sendInvitationEmail(CompanyInvitation invitation) {
        if (!smtpMailService.isConfigured()) {
            return;
        }

        String companyName = invitation.getCompany() != null && invitation.getCompany().getName() != null
            ? invitation.getCompany().getName()
            : "FindDem";
        String acceptUrl = firstFrontendOrigin() + "/accept-invitation/" + invitation.getInvitationToken();
        String departmentLine = invitation.getDepartment() != null
            ? "\nService : " + invitation.getDepartment().getName()
            : "";

        String subject = "Invitation a rejoindre " + companyName + " sur FindDem";
        String body = """
            Bonjour,

            Vous avez ete invite a rejoindre %s sur FindDem.

            Role : %s%s

            Pour creer votre acces, ouvrez ce lien :
            %s

            Ce lien expire dans 7 jours.

            A bientot,
            L'equipe FindDem
            """.formatted(companyName, invitation.getRole(), departmentLine, acceptUrl);

        smtpMailService.sendTextEmail(invitation.getEmail(), subject, body);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void expireIfNeeded(CompanyInvitation invitation) {
        if (
            invitation.getStatus() == CompanyInvitation.Status.PENDING
                && invitation.getExpiresAt() != null
                && invitation.getExpiresAt().isBefore(LocalDateTime.now())
        ) {
            invitation.setStatus(CompanyInvitation.Status.EXPIRED);
            invitationRepository.save(invitation);
        }
    }

    private CompanyInvitationDTO toDto(CompanyInvitation invitation) {
        CompanyInvitationDTO dto = new CompanyInvitationDTO();
        dto.setInvitationId(invitation.getInvitationId());
        if (invitation.getCompany() != null) {
            dto.setCompanyId(invitation.getCompany().getCompanyId());
            dto.setCompanyName(invitation.getCompany().getName());
        }
        if (invitation.getDepartment() != null) {
            dto.setDepartmentId(invitation.getDepartment().getDepartmentId());
            dto.setDepartmentName(invitation.getDepartment().getName());
        }
        dto.setEmail(invitation.getEmail());
        dto.setRole(invitation.getRole());
        dto.setStatus(invitation.getStatus().name());
        dto.setExpiresAt(invitation.getExpiresAt());
        dto.setCreatedAt(invitation.getCreatedAt());
        dto.setInvitationToken(invitation.getInvitationToken());
        dto.setAcceptUrl(firstFrontendOrigin() + "/accept-invitation/" + invitation.getInvitationToken());
        return dto;
    }

    private String firstFrontendOrigin() {
        String first = frontendUrl == null ? "http://localhost:3100" : frontendUrl.split(",")[0].trim();
        return first.isBlank() ? "http://localhost:3100" : first;
    }
}
