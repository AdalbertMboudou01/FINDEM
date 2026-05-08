package com.memoire.assistant.controller;

import com.memoire.assistant.model.Company;
import com.memoire.assistant.model.Department;
import com.memoire.assistant.model.Recruiter;
import com.memoire.assistant.repository.CompanyRepository;
import com.memoire.assistant.repository.DepartmentRepository;
import com.memoire.assistant.repository.RecruiterRepository;
import com.memoire.assistant.security.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final CompanyRepository companyRepository;
    private final RecruiterRepository recruiterRepository;

    public DepartmentController(DepartmentRepository departmentRepository,
                                CompanyRepository companyRepository,
                                RecruiterRepository recruiterRepository) {
        this.departmentRepository = departmentRepository;
        this.companyRepository = companyRepository;
        this.recruiterRepository = recruiterRepository;
    }

    // --- List all departments for current company ---
    @GetMapping
    public List<Map<String, Object>> listDepartments() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        List<Department> depts = departmentRepository.findByCompany_CompanyId(companyId);
        List<Recruiter> allRecruiters = recruiterRepository.findByCompany_CompanyId(companyId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Department d : depts) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("departmentId", d.getDepartmentId());
            dto.put("name", d.getName());
            dto.put("description", d.getDescription());
            dto.put("createdAt", d.getCreatedAt());
            long memberCount = allRecruiters.stream()
                .filter(r -> d.getDepartmentId().equals(
                    r.getDepartment() != null ? r.getDepartment().getDepartmentId() : null))
                .count();
            dto.put("memberCount", memberCount);
            result.add(dto);
        }
        return result;
    }

    // --- Create a department ---
    @PostMapping
    public ResponseEntity<Map<String, Object>> createDepartment(@RequestBody Map<String, String> body) {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        String name = body.getOrDefault("name", "").trim();
        if (name.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire");
        if (departmentRepository.existsByNameIgnoreCaseAndCompany_CompanyId(name, companyId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un service avec ce nom existe déjà");
        }

        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        Department dept = new Department();
        dept.setCompany(company);
        dept.setName(name);
        dept.setDescription(body.get("description"));
        dept = departmentRepository.save(dept);

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("departmentId", dept.getDepartmentId());
        dto.put("name", dept.getName());
        dto.put("description", dept.getDescription());
        dto.put("createdAt", dept.getCreatedAt());
        dto.put("memberCount", 0);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    // --- Update a department ---
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateDepartment(@PathVariable UUID id,
                                                                 @RequestBody Map<String, String> body) {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        Department dept = departmentRepository.findByDepartmentIdAndCompany_CompanyId(id, companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        String name = body.getOrDefault("name", "").trim();
        if (name.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire");
        dept.setName(name);
        dept.setDescription(body.get("description"));
        dept = departmentRepository.save(dept);

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("departmentId", dept.getDepartmentId());
        dto.put("name", dept.getName());
        dto.put("description", dept.getDescription());
        dto.put("createdAt", dept.getCreatedAt());
        return ResponseEntity.ok(dto);
    }

    // --- Delete a department ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable UUID id) {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        Department dept = departmentRepository.findByDepartmentIdAndCompany_CompanyId(id, companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Unassign members first
        List<Recruiter> members = recruiterRepository.findByCompany_CompanyId(companyId);
        for (Recruiter r : members) {
            if (r.getDepartment() != null && r.getDepartment().getDepartmentId().equals(id)) {
                r.setDepartment(null);
                recruiterRepository.save(r);
            }
        }
        departmentRepository.delete(dept);
        return ResponseEntity.noContent().build();
    }

    // --- Get members of a department ---
    @GetMapping("/{id}/members")
    public List<Map<String, Object>> getDepartmentMembers(@PathVariable UUID id) {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        departmentRepository.findByDepartmentIdAndCompany_CompanyId(id, companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return recruiterRepository.findByCompany_CompanyId(companyId).stream()
            .filter(r -> r.getDepartment() != null && r.getDepartment().getDepartmentId().equals(id))
            .map(this::toMemberDto)
            .toList();
    }

    // --- Assign a recruiter to a department ---
    @PutMapping("/{id}/members/{recruiterId}")
    public ResponseEntity<Void> assignMember(@PathVariable UUID id, @PathVariable UUID recruiterId) {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        Department dept = departmentRepository.findByDepartmentIdAndCompany_CompanyId(id, companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service introuvable"));

        Recruiter recruiter = recruiterRepository.findByRecruiterIdAndCompany_CompanyId(recruiterId, companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable"));

        recruiter.setDepartment(dept);
        recruiterRepository.save(recruiter);
        return ResponseEntity.ok().build();
    }

    // --- Remove a recruiter from a department ---
    @DeleteMapping("/{id}/members/{recruiterId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID id, @PathVariable UUID recruiterId) {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        Recruiter recruiter = recruiterRepository.findByRecruiterIdAndCompany_CompanyId(recruiterId, companyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membre introuvable"));

        if (recruiter.getDepartment() != null && recruiter.getDepartment().getDepartmentId().equals(id)) {
            recruiter.setDepartment(null);
            recruiterRepository.save(recruiter);
        }
        return ResponseEntity.noContent().build();
    }

    // --- List all company members with their department ---
    @GetMapping("/members")
    public List<Map<String, Object>> getAllMembers() {
        UUID companyId = TenantContext.getCompanyId();
        if (companyId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);

        return recruiterRepository.findByCompany_CompanyId(companyId).stream()
            .map(this::toMemberDto)
            .toList();
    }

    private Map<String, Object> toMemberDto(Recruiter r) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("recruiterId", r.getRecruiterId());
        dto.put("name", r.getName());
        dto.put("email", r.getEmail());
        dto.put("role", r.getRole());
        dto.put("status", r.getStatus());
        if (r.getDepartment() != null) {
            dto.put("departmentId", r.getDepartment().getDepartmentId());
            dto.put("departmentName", r.getDepartment().getName());
        } else {
            dto.put("departmentId", null);
            dto.put("departmentName", null);
        }
        return dto;
    }
}
