package com.memoire.assistant.repository;

import com.memoire.assistant.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    List<Department> findByCompany_CompanyId(UUID companyId);
    Optional<Department> findByDepartmentIdAndCompany_CompanyId(UUID departmentId, UUID companyId);
    boolean existsByNameIgnoreCaseAndCompany_CompanyId(String name, UUID companyId);
}
